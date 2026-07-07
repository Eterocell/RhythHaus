import AVFAudio
import Foundation
import Shared

/// Swift-owned AVAudioPlayer backend for KMP.
///
/// The delegate callback is event-based, so track-end auto-advance still fires when iOS locks the
/// screen/backgrounds the app and Kotlin polling may be suspended immediately after audio stops.
final class RhythHausAudioPlayerProvider: NSObject, IOSAudioPlayerProvider, AVAudioPlayerDelegate {
    var completionHandler: IOSAudioPlayerCompletionHandler?

    private var player: AVAudioPlayer?

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
        player?.play() ?? false
    }

    func pause() {
        player?.pause()
    }

    func stop() {
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
        player?.isPlaying ?? false
    }

    func fadeOutAndStop(fadeDurationSeconds: Double, silentVolume: Float) {
        guard let player else { return }
        player.setVolume(silentVolume, fadeDuration: fadeDurationSeconds)
        Thread.sleep(forTimeInterval: fadeDurationSeconds)
        player.stop()
        player.currentTime = 0
    }

    func audioPlayerDidFinishPlaying(_: AVAudioPlayer, successfully _: Bool) {
        completionHandler?.onPlaybackCompleted()
    }
}
