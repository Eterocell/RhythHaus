package com.eterocell.rhythhaus.mediastore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Host-neutral state of Android MediaStore audio access. */
public enum class MediaStoreAudioPermissionState {
    /** This host does not provide MediaStore audio access. */
    Unavailable,
    /** The host can show a permission request. */
    Requestable,
    /** Audio rows can be read. */
    Granted,
    /** The user must recover access in system app settings. */
    SettingsRequired,
}

/** Shared-facing MediaStore permission and recovery seam. */
public interface MediaStoreAudioPermissionController {
    /** Current host-neutral access state. */
    public val state: StateFlow<MediaStoreAudioPermissionState>

    /** Requests access when the host classifies it as requestable. */
    public fun requestPermission()

    /** Opens host settings when a request is no longer available. */
    public fun openSettings()
}

/** No-op controller for non-Android hosts and previews. */
public object UnavailableMediaStoreAudioPermissionController :
    MediaStoreAudioPermissionController {
    override val state: StateFlow<MediaStoreAudioPermissionState> =
        MutableStateFlow(MediaStoreAudioPermissionState.Unavailable)

    override fun requestPermission(): Unit = Unit

    override fun openSettings(): Unit = Unit
}
