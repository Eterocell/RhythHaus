package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionState

/**
 * Platform facts an Android host reads from the system before classifying
 * POST_NOTIFICATIONS. Every value is extracted by the host; the policy itself
 * never touches an Android framework API so it stays deterministically testable
 * on the JVM.
 */
internal data class NotificationPermissionFacts(
    val sdkAtLeastTiramisu: Boolean,
    val permissionGranted: Boolean,
    val hasRequestedBefore: Boolean,
    val shouldShowRationale: Boolean,
)

/**
 * Pure mapping from Android platform facts onto the Shared
 * [MediaNotificationPermissionState] contract.
 *
 * API < 33 and granted states never surface recovery. A denial is requestable
 * when the user has never been asked (the launch-time one-time request) or a
 * fresh request can still show the system dialog; a denial after a request with
 * no rationale left is settings-required.
 */
internal fun classifyMediaNotificationPermission(
    facts: NotificationPermissionFacts,
): MediaNotificationPermissionState =
    when {
        !facts.sdkAtLeastTiramisu ->
            MediaNotificationPermissionState.Unavailable
        facts.permissionGranted -> MediaNotificationPermissionState.Granted
        !facts.hasRequestedBefore ->
            MediaNotificationPermissionState.Requestable
        facts.shouldShowRationale ->
            MediaNotificationPermissionState.Requestable
        else -> MediaNotificationPermissionState.SettingsRequired
    }

/**
 * True only for the launch-time one-time request: denied on Android 13+, never
 * asked before, and no request already in flight. Every later recovery goes
 * through the Settings recovery action instead of an automatic request.
 */
internal fun shouldRequestOnLaunch(
    facts: NotificationPermissionFacts,
    requestInFlight: Boolean,
): Boolean =
    !requestInFlight &&
        !facts.hasRequestedBefore &&
        classifyMediaNotificationPermission(facts) ==
            MediaNotificationPermissionState.Requestable

/**
 * Re-requesting is allowed only while the state is
 * [MediaNotificationPermissionState.Requestable].
 */
internal fun canRequestPermission(
    state: MediaNotificationPermissionState,
): Boolean = state == MediaNotificationPermissionState.Requestable

/**
 * The application notification settings intent may be opened only for a
 * settings-required denial and only when the intent actually resolves, so the
 * host never launches an unresolvable settings route.
 */
internal fun shouldOpenNotificationSettings(
    state: MediaNotificationPermissionState,
    settingsIntentResolvable: Boolean,
): Boolean =
    state == MediaNotificationPermissionState.SettingsRequired &&
        settingsIntentResolvable
