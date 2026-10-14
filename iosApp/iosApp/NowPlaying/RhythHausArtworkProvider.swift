import Shared

/// Sets lockscreen / Control Center artwork for the now-playing track.
/// Called from the KMP IOSPlaybackEngine via NowPlayingArtworkBridge.
final class RhythHausArtworkProvider: NowPlayingArtworkProvider {
    var systemMediaPublisher: RhythHausSystemMediaPublisher?

    func setArtwork(
        trackTitle: String,
        artist: String,
        album: String?,
        artworkBytes: KotlinByteArray?
    ) {
        systemMediaPublisher?.updateArtwork(
            artworkBytes,
            trackTitle: trackTitle,
            artist: artist,
            album: album)
    }
}
