package com.eterocell.rhythhaus.playlistbackup

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.eterocell.rhythhaus.AudioSource
import java.io.File

/**
 * Probes local files and already-authorized content URIs without retaining
 * access.
 */
@Composable
public actual fun rememberPlaylistTrackReadability(): (AudioSource) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { source: AudioSource ->
            try {
                when (source) {
                    is AudioSource.FilePath -> {
                        val file = File(source.path)
                        file.isFile && file.inputStream().use { true }
                    }
                    is AudioSource.Uri ->
                        context.contentResolver
                            .openInputStream(Uri.parse(source.value))
                            ?.use { true } ?: false
                    is AudioSource.FileDescriptor -> false
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
