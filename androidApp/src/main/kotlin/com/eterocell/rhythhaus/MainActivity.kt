package com.eterocell.rhythhaus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    private val notificationPermissionController =
        AndroidNotificationPermissionController(this)
    private val mediaStorePermissionController =
        AndroidMediaStoreAudioPermissionController(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        notificationPermissionController.refresh()
        notificationPermissionController.requestPermissionOnLaunchIfNeeded()
        mediaStorePermissionController.refresh()

        setContent {
            App(
                notificationPermissionController =
                    notificationPermissionController,
                mediaStorePermissionController = mediaStorePermissionController,
            )
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
