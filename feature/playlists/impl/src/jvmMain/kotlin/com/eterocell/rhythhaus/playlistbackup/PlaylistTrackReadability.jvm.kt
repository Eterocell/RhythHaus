package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eterocell.rhythhaus.AudioSource
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/** Probes existing readable regular files without changing their contents. */
@Composable
public actual fun rememberPlaylistTrackReadability(): (AudioSource) -> Boolean =
    remember {
        ::isJvmPlaylistTrackReadable
    }

internal fun isJvmPlaylistTrackReadable(source: AudioSource): Boolean =
    try {
        val path =
            when (source) {
                is AudioSource.FilePath -> Path.of(source.path)
                is AudioSource.Uri -> Path.of(URI(source.value))
                is AudioSource.FileDescriptor -> null
            }
        path != null &&
            Files.isRegularFile(path) &&
            Files.isReadable(path) &&
            Files.newInputStream(path).use { true }
    } catch (_: Exception) {
        false
    }
