package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioMetadataReader
import com.eterocell.rhythhaus.AudioSource
import kotlin.coroutines.cancellation.CancellationException

sealed interface PlatformScanEvent {
    data class FolderVisited(val displayPath: String) : PlatformScanEvent
    data class AudioCandidate(val candidate: AudioScanCandidate) : PlatformScanEvent
    data class Skipped(
        val sourceLocalKey: String,
        val displayPath: String,
        val reason: String,
        val recoverable: Boolean,
    ) : PlatformScanEvent
}

fun interface PlatformAudioScanner {
    fun scan(source: LibrarySource): Sequence<PlatformScanEvent>
}

class LibraryScanner(
    private val repository: LibraryRepository,
    private val platformScanner: PlatformAudioScanner,
    private val metadataReader: AudioMetadataReader = AudioMetadataReader(),
    private val now: () -> Long,
    private val idFactory: (String) -> String,
) {
    fun scan(source: LibrarySource, isCancelled: () -> Boolean = { false }): ScanSession {
        val scanId = idFactory("scan")
        var session = ScanSession(
            id = scanId,
            sourceId = source.id,
            status = ScanStatus.Scanning,
            startedAtEpochMillis = now(),
        )

        repository.upsertSource(source)
        repository.insertScanSession(session)

        return try {
            for (event in platformScanner.scan(source)) {
                if (isCancelled()) {
                    session = session.cancelled()
                    repository.updateScanSession(session)
                    return session
                }

                session = when (event) {
                    is PlatformScanEvent.FolderVisited -> session.copy(
                        foldersVisited = session.foldersVisited + 1,
                    )

                    is PlatformScanEvent.Skipped -> session.recordSkipped(scanId, event)

                    is PlatformScanEvent.AudioCandidate -> session.importCandidate(scanId, event.candidate)
                }
                repository.updateScanSession(session)
            }

            if (isCancelled()) {
                session = session.cancelled()
            } else {
                val completedAt = now()
                session = session.copy(
                    status = ScanStatus.Completed,
                    completedAtEpochMillis = completedAt,
                )
                repository.upsertSource(source.copy(lastScanAtEpochMillis = completedAt))
            }
            repository.updateScanSession(session)
            session
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            session = session.copy(
                status = ScanStatus.Failed,
                completedAtEpochMillis = now(),
                terminalMessage = throwable.message ?: throwable::class.simpleName ?: "Scan failed",
            )
            repository.updateScanSession(session)
            session
        }
    }

    private fun ScanSession.recordSkipped(
        scanId: String,
        event: PlatformScanEvent.Skipped,
    ): ScanSession {
        repository.insertScanError(
            ScanError(
                id = idFactory("scan-error"),
                scanId = scanId,
                sourceLocalKey = event.sourceLocalKey,
                displayPath = event.displayPath,
                reason = event.reason,
                recoverable = event.recoverable,
                createdAtEpochMillis = now(),
            ),
        )
        return copy(
            filesVisited = filesVisited + 1,
            filesSkipped = filesSkipped + 1,
        )
    }

    private fun ScanSession.importCandidate(
        scanId: String,
        candidate: AudioScanCandidate,
    ): ScanSession {
        val track = candidate.toLibraryTrack(
            scanId = scanId,
            timestamp = now(),
            trackId = idFactory("track"),
            metadataReader = metadataReader,
        )
        return when (repository.upsertTrack(track)) {
            TrackUpsertResult.Added -> copy(
                filesVisited = filesVisited + 1,
                tracksAdded = tracksAdded + 1,
            )

            TrackUpsertResult.Updated -> copy(
                filesVisited = filesVisited + 1,
                tracksUpdated = tracksUpdated + 1,
            )
        }
    }

    private fun ScanSession.cancelled(): ScanSession = copy(
        status = ScanStatus.Cancelled,
        completedAtEpochMillis = now(),
        terminalMessage = "Scan cancelled",
    )
}

private fun AudioScanCandidate.toLibraryTrack(
    scanId: String,
    timestamp: Long,
    trackId: String,
    metadataReader: AudioMetadataReader,
): LibraryTrack {
    val resolvedSource = when (val source = metadataAudioSource) {
        is AudioSource.FilePath -> source.copy(path = resolvePathForMetadata(source.path))
        is AudioSource.Uri -> source
    }
    val metadata = try {
        runCatching { metadataReader.read(resolvedSource) }.getOrNull()
    } finally {
        cleanupMetadataAudioSource?.invoke()
    }
    return LibraryTrack(
        id = trackId,
        sourceId = sourceId,
        sourceLocalKey = sourceLocalKey,
        audioSource = audioSource,
        displayName = displayName,
        title = metadata?.title ?: displayName.fallbackTitle(),
        artist = metadata?.artist ?: "Local file",
        album = metadata?.album ?: "Imported audio",
        durationMillis = metadata?.durationMillis,
        sizeBytes = sizeBytes,
        modifiedAtEpochMillis = modifiedAtEpochMillis,
        lastSeenScanId = scanId,
        createdAtEpochMillis = timestamp,
        updatedAtEpochMillis = timestamp,
        trackNumber = metadata?.trackNumber,
        discNumber = metadata?.discNumber,
        artworkBytes = metadata?.artworkBytes,
        artworkMimeType = metadata?.artworkMimeType,
    )
}

private fun String.fallbackTitle(): String = substringBeforeLast('.', missingDelimiterValue = this)
    .replace('_', ' ')
    .replace('-', ' ')
    .trim()
    .ifBlank { "Untitled audio" }
