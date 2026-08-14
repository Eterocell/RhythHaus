import AVFAudio
import Foundation
import Shared

/// Swift-owned AVAudioPlayer backend for KMP.
///
/// The delegate callback is event-based, so track-end auto-advance still fires when iOS locks the
/// screen/backgrounds the app and Kotlin polling may be suspended immediately after audio stops.
final class RhythHausAudioPlayerProvider: NSObject, IOSAudioPlayerProvider, AVAudioPlayerDelegate {
    var completionHandler: IOSAudioPlayerCompletionHandler?
    var interruptionHandler: IOSAudioInterruptionHandler?

    private var player: AVAudioPlayer?
    private var isPlayingAuthoritatively = false
    private var notificationTokens: [NSObjectProtocol] = []

    override init() {
        super.init()
        let center = NotificationCenter.default
        notificationTokens.append(
            center.addObserver(
                forName: AVAudioSession.interruptionNotification,
                object: nil,
                queue: .main,
                using: { [weak self] notification in
                    self?.handleInterruption(notification)
                }))
        notificationTokens.append(
            center.addObserver(
                forName: AVAudioSession.routeChangeNotification,
                object: nil,
                queue: .main,
                using: { [weak self] notification in
                    self?.handleRouteChange(notification)
                }))
    }

    deinit {
        let center = NotificationCenter.default
        notificationTokens.forEach(center.removeObserver)
    }

    func load(filePath: String) -> Bool {
        stop()
        let url = URL(fileURLWithPath: filePath)
        do {
            let nextPlayer = try AVAudioPlayer(contentsOf: url)
            nextPlayer.delegate = self
            guard nextPlayer.prepareToPlay() else { return false }
            player = nextPlayer
            return true
        } catch {
            NSLog("[RhythHaus] Could not create AVAudioPlayer for %@: %@", filePath, String(describing: error))
            return false
        }
    }

    func play_() -> Bool {
        do {
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            return false
        }
        guard player?.play() == true else { return false }
        isPlayingAuthoritatively = true
        return true
    }

    func pause() {
        isPlayingAuthoritatively = false
        player?.pause()
    }

    func stop() {
        isPlayingAuthoritatively = false
        player?.stop()
        player?.currentTime = 0
    }

    func seekTo(positionMillis: Int64) {
        player?.currentTime = TimeInterval(positionMillis) / 1000.0
    }

    func currentPositionMillis() -> Int64 {
        Int64((player?.currentTime ?? 0) * 1000.0)
    }

    func currentDurationMillis() -> KotlinLong? {
        guard let duration = player?.duration, duration > 0 else { return nil }
        return KotlinLong(value: Int64(duration * 1000.0))
    }

    func isPlaying() -> Bool {
        isPlayingAuthoritatively
    }

    func fadeOutAndStop(fadeDurationSeconds: Double, silentVolume: Float) {
        isPlayingAuthoritatively = false
        guard let player else { return }
        player.setVolume(silentVolume, fadeDuration: fadeDurationSeconds)
        Thread.sleep(forTimeInterval: fadeDurationSeconds)
        player.stop()
        player.currentTime = 0
    }

    func audioPlayerDidFinishPlaying(_: AVAudioPlayer, successfully _: Bool) {
        isPlayingAuthoritatively = false
        completionHandler?.onPlaybackCompleted()
    }

    private func handleInterruption(_ notification: Notification) {
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
        guard
            let value = notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
            let reason = AVAudioSession.RouteChangeReason(rawValue: value),
            reason == .oldDeviceUnavailable,
            isPlayingAuthoritatively
        else { return }
        isPlayingAuthoritatively = false
        interruptionHandler?.onRouteDisconnected()
    }
}
