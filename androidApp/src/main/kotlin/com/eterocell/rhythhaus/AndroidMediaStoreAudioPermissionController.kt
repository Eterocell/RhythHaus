package com.eterocell.rhythhaus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionController
import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Android host owner for the neutral MediaStore audio permission seam. */
internal class AndroidMediaStoreAudioPermissionController(
    private val activity: ComponentActivity,
) : MediaStoreAudioPermissionController {
    private val preferences by lazy {
        activity.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )
    }

    private val _state = MutableStateFlow(classify())
    override val state: StateFlow<MediaStoreAudioPermissionState> = _state
    private var requestInFlight = false
    private val launcher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()) {
                requestInFlight = false
                refresh()
            }

    fun refresh() {
        _state.value = classify()
    }

    override fun requestPermission() {
        val facts = currentFacts()
        _state.value = classifyMediaStoreAudioPermission(facts)
        if (!canRequestMediaStoreAudioPermission(facts, requestInFlight)) return
        requestInFlight = true
        preferences.edit().putBoolean(requestedBeforeKey(), true).apply()
        launcher.launch(requiredPermission())
    }

    override fun openSettings() {
        val facts = currentFacts()
        _state.value = classifyMediaStoreAudioPermission(facts)
        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${activity.packageName}"),
            )
        if (shouldOpenMediaStoreAudioSettings(
            facts,
            settingsIntentResolvable =
                intent.resolveActivity(activity.packageManager) != null,
        )) {
            activity.startActivity(intent)
        }
    }

    private fun classify(): MediaStoreAudioPermissionState {
        return classifyMediaStoreAudioPermission(currentFacts())
    }

    private fun currentFacts(): MediaStoreAudioPermissionFacts {
        val permission = requiredPermission()
        return MediaStoreAudioPermissionFacts(
            permissionGranted =
                ContextCompat.checkSelfPermission(activity, permission) ==
                    PackageManager.PERMISSION_GRANTED,
            hasRequestedBefore =
                preferences.getBoolean(requestedBeforeKey(), false),
            shouldShowRationale =
                activity.shouldShowRequestPermissionRationale(permission),
        )
    }

    private fun requiredPermission(): String =
        when (mediaStoreAudioReadPermissionForSdk(Build.VERSION.SDK_INT)) {
            MediaStoreAudioReadPermission.ReadMediaAudio ->
                Manifest.permission.READ_MEDIA_AUDIO
            MediaStoreAudioReadPermission.ReadExternalStorage ->
                Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun requestedBeforeKey(): String =
        "${KEY_REQUESTED_BEFORE}.${requiredPermission()}"

    private companion object {
        const val PREFS_NAME = "media_store_audio_permission"
        const val KEY_REQUESTED_BEFORE = "read_media_audio_requested"
    }
}

/**
 * Platform facts used to classify the host-neutral MediaStore permission state.
 */
internal data class MediaStoreAudioPermissionFacts(
    val permissionGranted: Boolean,
    val hasRequestedBefore: Boolean,
    val shouldShowRationale: Boolean,
)

/** The Android permission required to query the external audio MediaStore. */
internal enum class MediaStoreAudioReadPermission {
    ReadMediaAudio,
    ReadExternalStorage,
}

/**
 * Selects the Android audio permission without coupling policy tests to Android
 * context.
 */
internal fun mediaStoreAudioReadPermissionForSdk(
    sdkInt: Int,
): MediaStoreAudioReadPermission =
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        MediaStoreAudioReadPermission.ReadMediaAudio
    } else {
        MediaStoreAudioReadPermission.ReadExternalStorage
    }

/**
 * Maps current Android permission facts into the Shared permission contract.
 */
internal fun classifyMediaStoreAudioPermission(
    facts: MediaStoreAudioPermissionFacts,
): MediaStoreAudioPermissionState =
    when {
        facts.permissionGranted -> MediaStoreAudioPermissionState.Granted
        !facts.hasRequestedBefore -> MediaStoreAudioPermissionState.Requestable
        facts.shouldShowRationale -> MediaStoreAudioPermissionState.Requestable
        else -> MediaStoreAudioPermissionState.SettingsRequired
    }

/** Allows only an explicit, non-overlapping runtime permission request. */
internal fun canRequestMediaStoreAudioPermission(
    facts: MediaStoreAudioPermissionFacts,
    requestInFlight: Boolean,
): Boolean =
    !requestInFlight &&
        classifyMediaStoreAudioPermission(facts) ==
            MediaStoreAudioPermissionState.Requestable

/** Opens application settings only for a current permanent denial. */
internal fun shouldOpenMediaStoreAudioSettings(
    facts: MediaStoreAudioPermissionFacts,
    settingsIntentResolvable: Boolean,
): Boolean =
    settingsIntentResolvable &&
        classifyMediaStoreAudioPermission(facts) ==
            MediaStoreAudioPermissionState.SettingsRequired
