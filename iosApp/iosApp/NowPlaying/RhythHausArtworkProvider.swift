import MediaPlayer
import Shared
import UIKit

/// Sets lockscreen / Control Center artwork for the now-playing track.
/// Called from the KMP IOSPlaybackEngine via NowPlayingArtworkBridge.
final class RhythHausArtworkProvider: NowPlayingArtworkProvider {
    func setArtwork(
        trackTitle _: String,
        artist _: String,
        album _: String?,
        artworkBytes: KotlinByteArray?
    ) {
        guard let bytes = artworkBytes else {
            removeArtworkFromNowPlaying()
            return
        }

        let count = Int(bytes.size)
        var byteArray = [UInt8](repeating: 0, count: count)
        for i in 0 ..< count {
            byteArray[i] = UInt8(truncatingIfNeeded: bytes.get(index: Int32(i)))
        }

        guard let image = UIImage(data: Data(byteArray)) else {
            removeArtworkFromNowPlaying()
            return
        }

        let artwork = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
        var info = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
        info[MPMediaItemPropertyArtwork] = artwork
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func removeArtworkFromNowPlaying() {
        var info = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
        info.removeValue(forKey: MPMediaItemPropertyArtwork)
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }
}
