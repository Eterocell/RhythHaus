package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.runtime.Composable
import com.eterocell.rhythhaus.AudioSource

/**
 * Returns a platform probe for local media readability before document export.
 */
@Composable
public expect fun rememberPlaylistTrackReadability(): (AudioSource) -> Boolean
