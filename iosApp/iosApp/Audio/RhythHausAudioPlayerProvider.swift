import AVFAudio
import Foundation
import Shared

/// Swift-owned AVAudioPlayer backend for KMP.
///
/// The delegate callback is event-based, so track-end auto-advance still fires when iOS locks the
/// screen/backgrounds the app and Kotlin polling may be suspended immediately after audio stops.
final class RhythHausAudioPlayerProvider: NSObject, IOSAudioPlayerProvider, AVAudioPlayerDelegate {
    var completionHandler: IOSAudioPlayerCompletionHandler? {
        willSet { Self.assertMainThread() }
    }
    var interruptionHandler: IOSAudioInterruptionHandler? {
        willSet { Self.assertMainThread() }
    }

    private var player: AVAudioPlayer?
    private var isPlayingAuthoritatively = false
    private var playRequestToken = 0
    private var notificationTokens: [NSObjectProtocol] = []

    override init() {
        Self.assertMainThread()
        super.init()
        let center = NotificationCenter.default
        let audioSession = AVAudioSession.sharedInstance()
        notificationTokens.append(
            center.addObserver(
                forName: AVAudioSession.interruptionNotification,
                object: audioSession,
                queue: .main,
                using: { [weak self] notification in
                    self?.handleInterruption(notification)
                }))
        notificationTokens.append(
            center.addObserver(
                forName: AVAudioSession.routeChangeNotification,
                object: audioSession,
                queue: .main,
                using: { [weak self] notification in
                    self?.handleRouteChange(notification)
                }))
    }

    deinit {
        Self.assertMainThread()
        let center = NotificationCenter.default
        notificationTokens.forEach(center.removeObserver)
    }

    func loadAsync(filePath: String, handler: IOSAudioPlayerLoadHandler) {
        Self.assertMainThread()
        stop()
        playRequestToken += 1
        let requestToken = playRequestToken
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            dispatchPrecondition(condition: .notOnQueue(.main))
            let preparedPlayer: AVAudioPlayer
            do {
                try AVAudioSession.sharedInstance().setCategory(
                    .playback,
                    mode: .default,
                    policy: .longFormAudio,
                    options: [])
                let candidate = try AVAudioPlayer(contentsOf: URL(fileURLWithPath: filePath))
                guard candidate.prepareToPlay() else {
                    DispatchQueue.main.async {
                        handler.onAudioLoadFailed()
                    }
                    return
                }
                preparedPlayer = candidate
            } catch {
                NSLog(
                    "[RhythHaus] Could not create AVAudioPlayer for %@: %@",
                    filePath,
                    String(describing: error))
                DispatchQueue.main.async {
                    handler.onAudioLoadFailed()
                }
                return
            }

            DispatchQueue.main.async {
                guard let self, self.playRequestToken == requestToken else {
                    handler.onAudioLoadFailed()
                    return
                }
                preparedPlayer.delegate = self
                self.player = preparedPlayer
                self.isPlayingAuthoritatively = false
                handler.onAudioLoaded()
            }
        }
    }

    func playAsync(handler: IOSAudioPlayerPlaybackStartHandler) {
        Self.assertMainThread()
        playRequestToken += 1
        let requestToken = playRequestToken
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            dispatchPrecondition(condition: .notOnQueue(.main))
            guard let self else { return }
            do {
                try AVAudioSession.sharedInstance().setActive(true)
            } catch {
                DispatchQueue.main.async {
                    handler.onPlaybackStartFailed()
                }
                return
            }
            DispatchQueue.main.async {
                guard self.playRequestToken == requestToken,
                      self.player?.play() == true
                else {
                    handler.onPlaybackStartFailed()
                    return
                }
                self.isPlayingAuthoritatively = true
                handler.onPlaybackStarted()
            }
        }
    }

    func pause() {
        Self.assertMainThread()
        playRequestToken += 1
        isPlayingAuthoritatively = false
        player?.pause()
    }

    func stop() {
        Self.assertMainThread()
        playRequestToken += 1
        isPlayingAuthoritatively = false
        player?.stop()
        player?.currentTime = 0
    }

    func seekTo(positionMillis: Int64) {
        Self.assertMainThread()
        player?.currentTime = TimeInterval(positionMillis) / 1000.0
    }

    func currentPositionMillis() -> Int64 {
        Self.assertMainThread()
        return Int64((player?.currentTime ?? 0) * 1000.0)
    }

    func currentDurationMillis() -> KotlinLong? {
        Self.assertMainThread()
        guard let duration = player?.duration, duration > 0 else { return nil }
        return KotlinLong(value: Int64(duration * 1000.0))
    }

    func isPlaying() -> Bool {
        Self.assertMainThread()
        return isPlayingAuthoritatively
    }

    func fadeOutAndStop(fadeDurationSeconds: Double, silentVolume: Float) {
        Self.assertMainThread()
        isPlayingAuthoritatively = false
        guard let player else { return }
        player.setVolume(silentVolume, fadeDuration: fadeDurationSeconds)
        Thread.sleep(forTimeInterval: fadeDurationSeconds)
        player.stop()
        player.currentTime = 0
    }

    func audioPlayerDidFinishPlaying(_: AVAudioPlayer, successfully _: Bool) {
        Self.assertMainThread()
        isPlayingAuthoritatively = false
        completionHandler?.onPlaybackCompleted()
    }

    private func handleInterruption(_ notification: Notification) {
        Self.assertMainThread()
        guard
            let value = notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
            let type = AVAudioSession.InterruptionType(rawValue: value)
        else { return }

        switch type {
        case .began:
            guard isPlayingAuthoritatively else { return }
            isPlayingAuthoritatively = false
            interruptionHandler?.onInterruptionBegan()
        case .ended:
            let optionsValue =
                notification.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
            let options = AVAudioSession.InterruptionOptions(rawValue: optionsValue)
            interruptionHandler?.onInterruptionEnded(
                shouldResume: options.contains(.shouldResume))
        @unknown default:
            break
        }
    }

    private func handleRouteChange(_ notification: Notification) {
        Self.assertMainThread()
        guard
            let value = notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
            let reason = AVAudioSession.RouteChangeReason(rawValue: value),
            reason == .oldDeviceUnavailable,
            isPlayingAuthoritatively
        else { return }
        isPlayingAuthoritatively = false
        interruptionHandler?.onRouteDisconnected()
    }

    private static func assertMainThread() {
        precondition(Thread.isMainThread, "RhythHausAudioPlayerProvider must run on the main thread")
    }
}
