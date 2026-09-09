package com.eterocell.rhythhaus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionController
import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android host controller for the Shared
 * [MediaNotificationPermissionController] contract.
 *
 * Owns every Android permission/rationale/intent API: runtime
 * [Manifest.permission.POST_NOTIFICATIONS] checks, the one-time launch
 * request, permission-result refresh, resume refresh, user re-request, and the
 * resolvable application-notification-settings intent. The controller only
 * classifies through [NotificationPermissionPolicy] and never touches
 * playback, queue, library, or in-app transport state.
 */
internal class AndroidNotificationPermissionController(
    private val activity: ComponentActivity,
) : MediaNotificationPermissionController {

    private val preferences by lazy {
        activity.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )
    }

    private val permissionLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ ->
            requestInFlight = false
            refresh()
        }

    private val mutableState =
        MutableStateFlow(MediaNotificationPermissionState.Unavailable)
    override val state: StateFlow<MediaNotificationPermissionState> =
        mutableState.asStateFlow()

    private var requestInFlight = false

    /** Recomputes the classified state from current platform facts. */
    fun refresh() {
        mutableState.value =
            classifyMediaNotificationPermission(currentFacts())
    }

    /**
     * Issues the launch-time one-time request when this launch qualifies and
     * no request is already in flight. All later recovery happens through
     * [requestPermission] and [openAppNotificationSettings].
     */
    fun requestPermissionOnLaunchIfNeeded() {
        if (shouldRequestOnLaunch(currentFacts(), requestInFlight)) {
            launchPermissionRequest()
        }
    }

    override fun requestPermission() {
        if (canRequestPermission(state.value) && !requestInFlight) {
            launchPermissionRequest()
        }
    }

    override fun openAppNotificationSettings() {
        val intent = appNotificationSettingsIntent()
        val resolvable = intent.resolveActivity(activity.packageManager) != null
        if (shouldOpenNotificationSettings(state.value, resolvable)) {
            activity.startActivity(intent)
        }
    }

    private fun launchPermissionRequest() {
        requestInFlight = true
        preferences.edit().putBoolean(KEY_REQUESTED_BEFORE, true).apply()
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun currentFacts(): NotificationPermissionFacts {
        val sdkAtLeastTiramisu =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val granted =
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        val requestedBefore =
            preferences.getBoolean(KEY_REQUESTED_BEFORE, false)
        val rationale =
            sdkAtLeastTiramisu &&
                activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS)
        return NotificationPermissionFacts(
            sdkAtLeastTiramisu = sdkAtLeastTiramisu,
            permissionGranted = granted,
            hasRequestedBefore = requestedBefore,
            shouldShowRationale = rationale,
        )
    }

    private fun appNotificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)

    private companion object {
        const val PREFS_NAME = "notification_permission"
        const val KEY_REQUESTED_BEFORE = "post_notifications_requested"
    }
}
