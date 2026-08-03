package com.eterocell.rhythhaus

/**
 * Protocol implemented in Swift to set lockscreen/Control Center artwork. KMP
 * cinterop doesn't expose NSData(bytes:length:) so the ByteArray → NSData →
 * UIImage → MPMediaItemArtwork chain must be built natively.
 */
public interface NowPlayingArtworkProvider {
    /** Updates lock-screen artwork metadata. */
    public fun setArtwork(
        trackTitle: String,
        artist: String,
        album: String?,
        artworkBytes: ByteArray?
    ): Unit
}

/**
 * Bridge object — the Swift app sets its provider in App.init().
 * IOSPlaybackEngine calls this from updateNowPlayingInfo().
 */
public object NowPlayingArtworkBridge {
    /** Swift-owned artwork backend used by playback. */
    public var provider: NowPlayingArtworkProvider? = null
}
