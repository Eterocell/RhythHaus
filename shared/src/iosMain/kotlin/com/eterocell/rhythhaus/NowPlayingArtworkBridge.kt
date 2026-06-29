package com.eterocell.rhythhaus

/**
 * Protocol implemented in Swift to set lockscreen/Control Center artwork.
 * KMP cinterop doesn't expose NSData(bytes:length:) so the
 * ByteArray → NSData → UIImage → MPMediaItemArtwork chain
 * must be built natively.
 */
interface NowPlayingArtworkProvider {
    fun setArtwork(trackTitle: String, artist: String, album: String?, artworkBytes: ByteArray?)
}

/**
 * Bridge object — the Swift app sets its provider in App.init().
 * IOSPlaybackEngine calls this from updateNowPlayingInfo().
 */
object NowPlayingArtworkBridge {
    var provider: NowPlayingArtworkProvider? = null
}
