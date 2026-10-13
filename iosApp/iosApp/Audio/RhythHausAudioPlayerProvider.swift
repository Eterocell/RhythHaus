import AVFAudio
import Foundation
import Shared

/// Two native voices beneath one audio session. All voice ownership and callbacks are main-thread
/// serialized; only file preparation and audio-session activation run on the private worker queue.
/// A started incoming voice remains reversible until explicit controller finish/cancel settlement.
final class RhythHausAudioPlayerProvider: NSObject, IOSAudioPlayerProvider {
    var completionHandler: IOSAudioPlayerCompletionHandler? {
        willSet { Self.assertMainThread() }
    }
    var interruptionHandler: IOSAudioInterruptionHandler? {
        willSet { Self.assertMainThread() }
    }


    func currentOutputNames() -> IOSAudioOutputSnapshot {
        Self.assertMainThread()
        return IOSAudioOutputSnapshot(
            names: audioSession.currentRoute.outputs.map(\.portName)
        )
    }

    private final class Voice {
        let player: AVAudioPlayer
        let filePath: String
        var gain: Float
        var playing = false
        var endedNaturally = false
        var completionDelegate: RhythHausVoiceCompletionDelegate?
        var completionHandler: IOSAudioPlayerCompletionHandler?
        var completionRun = 0

        init(player: AVAudioPlayer, filePath: String, gain: Float) {
            self.player = player
            self.filePath = filePath
            self.gain = gain
            player.volume = gain
        }
    }

    private var current: Voice?
    private var preparedIncoming: Voice?
    private var retiring: Voice?
    private var retiringOriginalGain: Float?
    private var playbackGain: Float = 1
    private var currentFilePath: String?
    private var stoppedPosition: TimeInterval = 0
    private var retainedDuration: TimeInterval?
    private var loadRequestToken = 0
    private var playRequestToken = 0
    private var incomingRequestToken = 0
    private let audioSession = AVAudioSession.sharedInstance()
    private let audioQueue = DispatchQueue(label: "com.eterocell.rhythhaus.audio", qos: .userInitiated)
    // Accessed only on audioQueue, including first configuration before a stopped voice reloads.
    private var sessionConfigured = false
    private var notificationTokens: [NSObjectProtocol] = []

#if DEBUG
    var debugPlayerVolume: Float? {
        Self.assertMainThread()
        return current?.player.volume
    }

    var debugCurrentPlayer: AVAudioPlayer? {
        Self.assertMainThread()
        return current?.player
    }

    var debugPreparedPlayer: AVAudioPlayer? {
        Self.assertMainThread()
        return preparedIncoming?.player
    }

    var debugRetiringPlayer: AVAudioPlayer? {
        Self.assertMainThread()
        return retiring?.player
    }

    var debugVoiceCount: Int {
        Self.assertMainThread()
        return (current == nil ? 0 : 1) + (preparedIncoming == nil ? 0 : 1) + (retiring == nil ? 0 : 1)
    }

    var debugAudioSession: AVAudioSession { audioSession }
    var debugNotificationObserverCount: Int { notificationTokens.count }
#endif

    override init() {
        Self.assertMainThread()
        super.init()
        let center = NotificationCenter.default
        notificationTokens.append(
            center.addObserver(
                forName: AVAudioSession.interruptionNotification,
                object: audioSession,
                queue: .main,
                using: { [weak self] notification in self?.handleInterruption(notification) }
            )
        )
        notificationTokens.append(
            center.addObserver(
                forName: AVAudioSession.routeChangeNotification,
                object: audioSession,
                queue: .main,
                using: { [weak self] notification in self?.handleRouteChange(notification) }
            )
        )
    }

    deinit {
        Self.assertMainThread()
        let center = NotificationCenter.default
        notificationTokens.forEach(center.removeObserver)
        dispose(current)
        dispose(preparedIncoming)
        dispose(retiring)
    }

    func loadAsync(filePath: String, handler: IOSAudioPlayerLoadHandler) {
        Self.assertMainThread()
        disposeAll(resetGain: false, forgetMedia: true)
        let requestToken = loadRequestToken
        audioQueue.async { [weak self] in
            guard let self else {
                DispatchQueue.main.async { handler.onAudioLoadFailed() }
                return
            }
            do {
                let candidate = try self.preparePlayer(filePath: filePath)
                DispatchQueue.main.async {
                    guard self.loadRequestToken == requestToken else {
                        candidate.stop()
                        handler.onAudioLoadFailed()
                        return
                    }
                    self.current = Voice(player: candidate, filePath: filePath, gain: self.playbackGain)
                    self.currentFilePath = filePath
                    self.retainedDuration = candidate.duration
                    self.stoppedPosition = 0
                    handler.onAudioLoaded()
                }
            } catch {
                DispatchQueue.main.async { self.reportLoadFailure(filePath: filePath, error: error, handler: handler) }
            }
        }
    }

    func prepareIncomingAsync(filePath: String, handler: IOSAudioPlayerLoadHandler) {
        Self.assertMainThread()
        guard current != nil, retiring == nil else {
            handler.onAudioLoadFailed()
            return
        }
        cancelPreparedIncoming()
        let requestToken = incomingRequestToken
        let outgoing = current
        audioQueue.async { [weak self] in
            guard let self else {
                DispatchQueue.main.async { handler.onAudioLoadFailed() }
                return
            }
            do {
                let candidate = try self.preparePlayer(filePath: filePath)
                DispatchQueue.main.async {
                    guard self.incomingRequestToken == requestToken,
                          self.current === outgoing, self.retiring == nil
                    else {
                        candidate.stop()
                        handler.onAudioLoadFailed()
                        return
                    }
                    self.preparedIncoming = Voice(player: candidate, filePath: filePath, gain: 0)
                    handler.onAudioLoaded()
                }
            } catch {
                DispatchQueue.main.async { self.reportLoadFailure(filePath: filePath, error: error, handler: handler) }
            }
        }
    }

    func incomingDurationMillis() -> KotlinLong? {
        Self.assertMainThread()
        guard let duration = preparedIncoming?.player.duration, duration > 0 else { return nil }
        return KotlinLong(value: Int64(duration * 1000))
    }

    func playAsync(handler: IOSAudioPlayerPlaybackStartHandler) {
        Self.assertMainThread()
        guard let filePath = currentFilePath else {
            handler.onPlaybackStartFailed()
            return
        }
        playRequestToken += 1
        let requestToken = playRequestToken
        let voice = current
        let completionRun = (voice?.completionRun ?? 0) + 1
        // Kotlin has already admitted a new run before asynchronous activation begins.
        // A queued previous delegate must not stop/reset this voice during that interval.
        voice?.completionRun = completionRun
        // Capture the run's handler before leaving the main owner. Never read the mutable bridge
        // handler from a delegate belonging to a retiring or previously played voice.
        let completion = completionHandler
        audioQueue.async { [weak self] in
            guard let self else {
                DispatchQueue.main.async { handler.onPlaybackStartFailed() }
                return
            }
            do {
                try self.configureSessionIfNeeded()
                let reloaded: AVAudioPlayer?
                if voice == nil {
                    reloaded = try self.preparePlayer(filePath: filePath)
                } else {
                    reloaded = nil
                }
                try self.audioSession.setActive(true)
                DispatchQueue.main.async {
                    guard self.playRequestToken == requestToken,
                          self.current === voice,
                          self.currentFilePath == filePath
                    else {
                        reloaded?.stop()
                        handler.onPlaybackStartFailed()
                        return
                    }
                    let active: Voice
                    if let voice {
                        active = voice
                    } else if let reloaded {
                        active = Voice(player: reloaded, filePath: filePath, gain: self.playbackGain)
                        active.completionRun = completionRun
                        active.player.currentTime = self.stoppedPosition
                        self.current = active
                        self.retainedDuration = reloaded.duration
                    } else {
                        handler.onPlaybackStartFailed()
                        return
                    }
                    active.gain = self.playbackGain
                    active.endedNaturally = false
                    active.player.volume = active.gain
                    self.retiring?.player.volume = self.retiring?.gain ?? 0
                    self.installCompletion(on: active, run: completionRun, handler: completion)
                    guard active.player.play() else {
                        handler.onPlaybackStartFailed()
                        return
                    }
                    active.playing = true
                    if let retiring = self.retiring, !retiring.endedNaturally {
                        retiring.playing = retiring.player.play()
                        if !retiring.playing {
                            retiring.completionDelegate?.audioPlayerDecodeErrorDidOccur(retiring.player, error: nil)
                        }
                    }
                    handler.onPlaybackStarted()
                }
            } catch {
                DispatchQueue.main.async { self.reportStartFailure(handler) }
            }
        }
    }

    func startIncomingAsync(
        completionHandler: IOSAudioPlayerCompletionHandler,
        handler: IOSAudioPlayerPlaybackStartHandler
    ) {
        Self.assertMainThread()
        guard let incoming = preparedIncoming, let outgoing = current, retiring == nil else {
            handler.onPlaybackStartFailed()
            return
        }
        playRequestToken += 1
        let requestToken = playRequestToken
        let incomingToken = incomingRequestToken
        let completionRun = incoming.completionRun + 1
        incoming.completionRun = completionRun
        audioQueue.async { [weak self] in
            guard let self else {
                DispatchQueue.main.async { handler.onPlaybackStartFailed() }
                return
            }
            do {
                try self.configureSessionIfNeeded()
                try self.audioSession.setActive(true)
                DispatchQueue.main.async {
                    guard self.playRequestToken == requestToken,
                          self.incomingRequestToken == incomingToken,
                          self.preparedIncoming === incoming,
                          self.current === outgoing,
                          self.retiring == nil
                    else {
                        handler.onPlaybackStartFailed()
                        return
                    }
                    incoming.player.volume = incoming.gain
                    self.installCompletion(on: incoming, run: completionRun, handler: completionHandler)
                    guard incoming.player.play() else {
                        self.cancelPreparedIncoming()
                        handler.onPlaybackStartFailed()
                        return
                    }
                    incoming.playing = true
                    self.retiring = outgoing
                    self.retiringOriginalGain = outgoing.gain
                    self.current = incoming
                    self.preparedIncoming = nil
                    self.currentFilePath = incoming.filePath
                    self.retainedDuration = incoming.player.duration
                    self.stoppedPosition = 0
                    self.playbackGain = incoming.gain
                    self.completionHandler = completionHandler
                    handler.onPlaybackStarted()
                }
            } catch {
                DispatchQueue.main.async {
                    if self.playRequestToken == requestToken,
                       self.incomingRequestToken == incomingToken,
                       self.preparedIncoming === incoming {
                        self.cancelPreparedIncoming()
                    }
                    self.reportStartFailure(handler)
                }
            }
        }
    }

    func setCrossfadeGains(outgoingGain: Float, incomingGain: Float) {
        Self.assertMainThread()
        Self.requireGain(outgoingGain)
        Self.requireGain(incomingGain)
        if let retiring {
            retiring.gain = outgoingGain
            retiring.player.volume = outgoingGain
            playbackGain = incomingGain
            current?.gain = incomingGain
            current?.player.volume = incomingGain
        } else {
            playbackGain = outgoingGain
            current?.gain = outgoingGain
            current?.player.volume = outgoingGain
            preparedIncoming?.gain = incomingGain
            preparedIncoming?.player.volume = incomingGain
        }
    }

    func finishCrossfade() {
        Self.assertMainThread()
        dispose(retiring)
        retiring = nil
        retiringOriginalGain = nil
    }

    func finishGapless() {
        Self.assertMainThread()
        dispose(retiring)
        retiring = nil
        retiringOriginalGain = nil
    }

    func cancelPreparedIncoming() {
        Self.assertMainThread()
        incomingRequestToken += 1
        dispose(preparedIncoming)
        preparedIncoming = nil
        guard let outgoing = retiring else { return }
        // Native Playing is only a proposal. Retain the original player and delegate until
        // controller admission commits retirement; rejection must not replay outgoing audio.
        playRequestToken += 1
        dispose(current)
        current = outgoing
        retiring = nil
        outgoing.gain = retiringOriginalGain ?? outgoing.gain
        outgoing.player.volume = outgoing.gain
        playbackGain = outgoing.gain
        currentFilePath = outgoing.filePath
        retainedDuration = outgoing.player.duration
        stoppedPosition = outgoing.player.currentTime
        completionHandler = outgoing.completionHandler
        retiringOriginalGain = nil
    }

    func pause() {
        Self.assertMainThread()
        playRequestToken += 1
        current?.playing = false
        retiring?.playing = false
        current?.player.pause()
        retiring?.player.pause()
    }

    func stop() {
        Self.assertMainThread()
        disposeAll(resetGain: true, forgetMedia: false)
    }

    func setPlaybackGain(gain: Float) {
        Self.assertMainThread()
        Self.requireGain(gain)
        playbackGain = gain
        current?.gain = gain
        current?.player.volume = gain
    }

    func seekTo(positionMillis: Int64) {
        Self.assertMainThread()
        finishCrossfade()
        cancelPreparedIncoming()
        stoppedPosition = max(0, TimeInterval(positionMillis) / 1000)
        current?.endedNaturally = false
        current?.player.currentTime = stoppedPosition
    }

    func currentPositionMillis() -> Int64 {
        Self.assertMainThread()
        let position: TimeInterval
        if let current {
            position = current.endedNaturally ? current.player.duration : current.player.currentTime
        } else {
            position = stoppedPosition
        }
        return Int64(position * 1000)
    }

    func currentDurationMillis() -> KotlinLong? {
        Self.assertMainThread()
        guard let duration = current?.player.duration ?? retainedDuration, duration > 0 else { return nil }
        return KotlinLong(value: Int64(duration * 1000))
    }

    func isPlaying() -> Bool {
        Self.assertMainThread()
        return current?.playing ?? false
    }

    func fadeOutAndStop(fadeDurationSeconds: Double, silentVolume: Float) {
        Self.assertMainThread()
        Self.requireGain(silentVolume)
        let hadAudibleVoice = current?.playing == true || retiring?.playing == true
        current?.playing = false
        retiring?.playing = false
        current?.player.setVolume(silentVolume, fadeDuration: fadeDurationSeconds)
        retiring?.player.setVolume(silentVolume, fadeDuration: fadeDurationSeconds)
        if hadAudibleVoice { Thread.sleep(forTimeInterval: fadeDurationSeconds) }
        disposeAll(resetGain: false, forgetMedia: true)
    }

    private func installCompletion(on voice: Voice, run: Int, handler: IOSAudioPlayerCompletionHandler?) {
        let delegate = RhythHausVoiceCompletionDelegate { [weak self, weak voice] player, successfully in
            guard let self, let voice, voice.player === player,
                  voice.completionRun == run,
                  self.current === voice || self.retiring === voice
            else { return }
            voice.playing = false
            voice.endedNaturally = successfully
            if !successfully { voice.player.stop() }
            if successfully { handler?.onPlaybackCompleted() } else { handler?.onPlaybackFailed() }
        }
        voice.completionHandler = handler
        voice.completionDelegate = delegate
        voice.player.delegate = delegate
    }

    private func dispose(_ voice: Voice?) {
        voice?.completionRun += 1
        voice?.playing = false
        voice?.player.delegate = nil
        voice?.player.stop()
        voice?.completionDelegate = nil
        voice?.completionHandler = nil
    }

    private func disposeAll(resetGain: Bool, forgetMedia: Bool) {
        loadRequestToken += 1
        playRequestToken += 1
        incomingRequestToken += 1
        dispose(current)
        dispose(preparedIncoming)
        dispose(retiring)
        current = nil
        preparedIncoming = nil
        retiring = nil
        retiringOriginalGain = nil
        completionHandler = nil
        stoppedPosition = 0
        if resetGain { playbackGain = 1 }
        if forgetMedia {
            currentFilePath = nil
            retainedDuration = nil
        }
    }

    private func configureSessionIfNeeded() throws {
        dispatchPrecondition(condition: .onQueue(audioQueue))
        if !sessionConfigured {
            try audioSession.setCategory(.playback, mode: .default, policy: .longFormAudio, options: [])
            sessionConfigured = true
        }
    }

    private func preparePlayer(filePath: String) throws -> AVAudioPlayer {
        try configureSessionIfNeeded()
        let candidate = try AVAudioPlayer(contentsOf: URL(fileURLWithPath: filePath))
        guard candidate.prepareToPlay() else { throw AudioPreparationFailure.unavailable }
        return candidate
    }

    private func reportLoadFailure(filePath: String, error: Error, handler: IOSAudioPlayerLoadHandler) {
        Self.assertMainThread()
        NSLog("[RhythHaus] Could not create AVAudioPlayer for %@: %@", filePath, String(describing: error))
        handler.onAudioLoadFailed()
    }

    private func reportStartFailure(_ handler: IOSAudioPlayerPlaybackStartHandler) {
        Self.assertMainThread()
        handler.onPlaybackStartFailed()
    }

    private func handleInterruption(_ notification: Notification) {
        Self.assertMainThread()
        guard let value = notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: value)
        else { return }
        switch type {
        case .began:
            let wasPlaying = current?.playing == true || retiring?.playing == true
            pause()
            if wasPlaying { interruptionHandler?.onInterruptionBegan() }
        case .ended:
            let optionsValue = notification.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
            let options = AVAudioSession.InterruptionOptions(rawValue: optionsValue)
            interruptionHandler?.onInterruptionEnded(shouldResume: options.contains(.shouldResume))
        @unknown default:
            break
        }
    }

    private func handleRouteChange(_ notification: Notification) {
        Self.assertMainThread()
        interruptionHandler?.onRouteChanged()
        guard let value = notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
              let reason = AVAudioSession.RouteChangeReason(rawValue: value), reason == .oldDeviceUnavailable
        else { return }
        pause()
        interruptionHandler?.onRouteDisconnected()
    }

    private static func requireGain(_ gain: Float) {
        precondition(gain.isFinite && gain >= 0 && gain <= 1, "Playback gain must be finite and within 0...1")
    }

    private static func assertMainThread() {
        precondition(Thread.isMainThread, "RhythHausAudioPlayerProvider must run on the main thread")
    }
}

private enum AudioPreparationFailure: Error {
    case unavailable
}

/// The callback owns the player/run handler captured at play admission, not a mutable current token.
private final class RhythHausVoiceCompletionDelegate: NSObject, AVAudioPlayerDelegate {
    private let finished: (AVAudioPlayer, Bool) -> Void
    private var reported = false

    init(finished: @escaping (AVAudioPlayer, Bool) -> Void) {
        self.finished = finished
    }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        deliver(player, successfully: flag)
    }

    func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error _: Error?) {
        deliver(player, successfully: false)
    }

    private func deliver(_ player: AVAudioPlayer, successfully: Bool) {
        if !Thread.isMainThread {
            DispatchQueue.main.async { self.deliver(player, successfully: successfully) }
            return
        }
        guard !reported else { return }
        reported = true
        finished(player, successfully)
    }
}
