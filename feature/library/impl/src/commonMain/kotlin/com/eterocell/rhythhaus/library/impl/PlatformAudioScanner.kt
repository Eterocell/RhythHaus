package com.eterocell.rhythhaus.library.impl

import com.eterocell.rhythhaus.library.AudioScanCandidate
import com.eterocell.rhythhaus.library.LibrarySource

/** Events produced while scanning a library source. */
sealed interface PlatformScanEvent {
    /** A folder was visited during the scan. */
    data class FolderVisited(val displayPath: String) : PlatformScanEvent

    /** An audio file candidate was discovered. */
    data class AudioCandidate(val candidate: AudioScanCandidate) :
        PlatformScanEvent

    /** An item was skipped during the scan. */
    data class Skipped(
        val sourceLocalKey: String,
        val displayPath: String,
        val reason: String,
        val recoverable: Boolean,
    ) : PlatformScanEvent
}

/** Platform scanner that yields a lazy sequence of scan events. */
fun interface PlatformAudioScanner {
    /**
     * Scans the given source and yields its events.
     *
     * @param source the library source to scan.
     */
    fun scan(source: LibrarySource): Sequence<PlatformScanEvent>
}
