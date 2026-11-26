package com.eterocell.rhythhaus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    private val notificationPermissionController =
        AndroidNotificationPermissionController(this)
    private lateinit var mediaStorePermissionController:
        AndroidMediaStoreAudioPermissionController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        mediaStorePermissionController =
            AndroidMediaStoreAudioPermissionController(this)
        notificationPermissionController.refresh()
        notificationPermissionController.requestPermissionOnLaunchIfNeeded()
        mediaStorePermissionController.refresh()

        setContent { RhythHausContent() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun RhythHausContent() {
        val content: @Composable () -> Unit = {
            App(
                notificationPermissionController = notificationPermissionController,
                mediaStorePermissionController = mediaStorePermissionController,
            )
        }
        if (BuildConfig.PROFILE_JOURNEYS) {
            Box(Modifier.semantics { testTagsAsResourceId = true }) { content() }
        } else {
            content()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh after returning from the system permission dialog or the
        // application notification settings so the recovery surface follows
        // the authoritative permission state.
        notificationPermissionController.refresh()
        mediaStorePermissionController.refresh()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
