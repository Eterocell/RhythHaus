package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.taglib.RhythHausTagLib
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagMetadata
import com.eterocell.rhythhaus.taglib.TagReadResult
import com.eterocell.rhythhaus.taglib.createTagLibReader

data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMillis: Long? = null,
)

class AudioMetadataReader(
    private val tagLibReader: TagLibReader = createTagLibReader(),
) {
    fun read(source: AudioSource): AudioMetadata? {
        val path = when (source) {
            is AudioSource.FilePath -> source.path
            is AudioSource.Uri -> return null
        }

        return when (val result = RhythHausTagLib.readPath(path, tagLibReader)) {
            is TagReadResult.Found -> result.metadata.toAudioMetadata()
            is TagReadResult.Unsupported -> null
            is TagReadResult.Failed -> null
        }
    }
}

fun enrichImportedAudioFiles(
    files: List<ImportedAudioFile>,
    metadataReader: AudioMetadataReader = AudioMetadataReader(),
): List<ImportedAudioFile> = files.map { file ->
    if (file.metadata != null) {
        file
    } else {
        file.copy(metadata = metadataReader.read(file.source))
    }
}

private fun TagMetadata.toAudioMetadata(): AudioMetadata = AudioMetadata(
    title = title.normalizedOrNull(),
    artist = artist.normalizedOrNull(),
    album = album.normalizedOrNull(),
    durationMillis = durationMillis?.takeIf { it > 0L },
)

private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf { it.isNotBlank() }
