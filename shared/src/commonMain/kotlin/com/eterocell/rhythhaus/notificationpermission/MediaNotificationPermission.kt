package com.eterocell.rhythhaus.notificationpermission

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Android media-notification permission surface used by the app shell.
 *
 * The contract is deliberately Android-free: non-Android call sites (iOS,
 * desktop) keep the [Unavailable] default instead of importing an Android
 * framework type, while an Android host maps platform permission facts onto
 * these exact states. Recovery never alters playback, queue, library, or in-app
 * transport state.
 */
enum class MediaNotificationPermissionState {
    /**
     * No Android media-notification permission exists (API < 33 or non-Android
     * host).
     */
    Unavailable,

    /** POST_NOTIFICATIONS is granted; no recovery surface is needed. */
    Granted,

    /**
     * POST_NOTIFICATIONS is denied but another request can still show the
     * system dialog.
     */
    Requestable,

    /**
     * POST_NOTIFICATIONS is denied and only the app notification settings can
     * restore it.
     */
    SettingsRequired,
}

/**
 * Android-free bridge between an Android host and the shared composition root.
 *
 * The host updates [state] as platform facts change (startup, permission
 * results, resume after settings). [requestPermission] issues one request and
 * suppresses concurrent requests; [openAppNotificationSettings] opens the
 * application notification settings only when that settings route is the
 * correct recovery and the intent is resolvable. Non-Android hosts use
 * [UnavailableMediaNotificationPermissionController], whose state never leaves
 * [MediaNotificationPermissionState.Unavailable].
 */
interface MediaNotificationPermissionController {
    /** Latest classified permission state. */
    val state: StateFlow<MediaNotificationPermissionState>

    /**
     * Requests POST_NOTIFICATIONS when the state is
     * [MediaNotificationPermissionState.Requestable].
     */
    fun requestPermission()

    /**
     * Opens the Android application notification settings when the state
     * requires it.
     */
    fun openAppNotificationSettings()
}

/**
 * Default controller for hosts without an Android media-notification permission
 * surface. Keeps [state] at [MediaNotificationPermissionState.Unavailable] and
 * makes both actions no-ops, so iOS and desktop call sites are unchanged.
 */
object UnavailableMediaNotificationPermissionController :
    MediaNotificationPermissionController {
    override val state: StateFlow<MediaNotificationPermissionState> =
        MutableStateFlow(MediaNotificationPermissionState.Unavailable)

    override fun requestPermission() = Unit

    override fun openAppNotificationSettings() = Unit
}
