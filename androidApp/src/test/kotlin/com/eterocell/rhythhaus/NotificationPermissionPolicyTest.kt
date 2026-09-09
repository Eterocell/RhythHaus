package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionState.Granted
import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionState.Requestable
import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionState.SettingsRequired
import com.eterocell.rhythhaus.notificationpermission.MediaNotificationPermissionState.Unavailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPermissionPolicyTest {

    private fun facts(
        sdkAtLeastTiramisu: Boolean,
        permissionGranted: Boolean,
        hasRequestedBefore: Boolean,
        shouldShowRationale: Boolean,
    ) =
        NotificationPermissionFacts(
            sdkAtLeastTiramisu = sdkAtLeastTiramisu,
            permissionGranted = permissionGranted,
            hasRequestedBefore = hasRequestedBefore,
            shouldShowRationale = shouldShowRationale,
        )

    @Test
    fun belowTiramisuIsUnavailableAndNeverRequests() {
        assertEquals(
            Unavailable,
            classifyMediaNotificationPermission(
                facts(
                    sdkAtLeastTiramisu = false,
                    permissionGranted = false,
                    hasRequestedBefore = false,
                    shouldShowRationale = false,
                )),
        )
        assertEquals(
            Unavailable,
            classifyMediaNotificationPermission(
                facts(
                    sdkAtLeastTiramisu = false,
                    permissionGranted = true,
                    hasRequestedBefore = false,
                    shouldShowRationale = false,
                )),
        )
        assertFalse(
            shouldRequestOnLaunch(
                facts(
                    sdkAtLeastTiramisu = false,
                    permissionGranted = false,
                    hasRequestedBefore = false,
                    shouldShowRationale = false,
                ),
                requestInFlight = false,
            ),
        )
    }

    @Test
    fun grantedOnTiramisuIsGrantedForEveryDenialHistory() {
        listOf(
                facts(
                    sdkAtLeastTiramisu = true,
                    permissionGranted = true,
                    hasRequestedBefore = false,
                    shouldShowRationale = false,
                ),
                facts(
                    sdkAtLeastTiramisu = true,
                    permissionGranted = true,
                    hasRequestedBefore = true,
                    shouldShowRationale = false,
                ),
                facts(
                    sdkAtLeastTiramisu = true,
                    permissionGranted = true,
                    hasRequestedBefore = true,
                    shouldShowRationale = true,
                ),
            )
            .forEach { grantedFacts ->
                assertEquals(
                    Granted, classifyMediaNotificationPermission(grantedFacts))
                assertFalse(
                    shouldRequestOnLaunch(
                        grantedFacts, requestInFlight = false))
            }
    }

    @Test
    fun neverAskedDenialIsRequestableAndRequestsOnceOnLaunch() {
        val neverAsked =
            facts(
                sdkAtLeastTiramisu = true,
                permissionGranted = false,
                hasRequestedBefore = false,
                shouldShowRationale = false,
            )
        assertEquals(
            Requestable, classifyMediaNotificationPermission(neverAsked))
        assertTrue(shouldRequestOnLaunch(neverAsked, requestInFlight = false))
        assertTrue(canRequestPermission(Requestable))
    }

    @Test
    fun requestableDenialOffersReRequestButNeverAutoRequests() {
        val deniedOnce =
            facts(
                sdkAtLeastTiramisu = true,
                permissionGranted = false,
                hasRequestedBefore = true,
                shouldShowRationale = true,
            )
        assertEquals(
            Requestable, classifyMediaNotificationPermission(deniedOnce))
        assertFalse(shouldRequestOnLaunch(deniedOnce, requestInFlight = false))
        assertTrue(canRequestPermission(Requestable))
    }

    @Test
    fun permanentDenialIsSettingsRequiredAndNeverRequests() {
        val permanentDenial =
            facts(
                sdkAtLeastTiramisu = true,
                permissionGranted = false,
                hasRequestedBefore = true,
                shouldShowRationale = false,
            )
        assertEquals(
            SettingsRequired,
            classifyMediaNotificationPermission(permanentDenial),
        )
        assertFalse(
            shouldRequestOnLaunch(permanentDenial, requestInFlight = false),
        )
        assertFalse(canRequestPermission(SettingsRequired))
        assertFalse(canRequestPermission(Granted))
        assertFalse(canRequestPermission(Unavailable))
    }

    @Test
    fun launchRequestIsSuppressedWhileAnotherRequestIsInFlight() {
        val neverAsked =
            facts(
                sdkAtLeastTiramisu = true,
                permissionGranted = false,
                hasRequestedBefore = false,
                shouldShowRationale = false,
            )
        assertFalse(
            shouldRequestOnLaunch(neverAsked, requestInFlight = true),
        )
    }

    @Test
    fun permissionResultRefreshMapsGrantToGranted() {
        assertEquals(
            Granted,
            classifyMediaNotificationPermission(
                facts(
                    sdkAtLeastTiramisu = true,
                    permissionGranted = true,
                    hasRequestedBefore = true,
                    shouldShowRationale = false,
                )),
        )
    }

    @Test
    fun permissionResultRefreshMapsDenyToRequestableWhenRationaleRemains() {
        assertEquals(
            Requestable,
            classifyMediaNotificationPermission(
                facts(
                    sdkAtLeastTiramisu = true,
                    permissionGranted = false,
                    hasRequestedBefore = true,
                    shouldShowRationale = true,
                )),
        )
    }

    @Test
    fun permissionResultRefreshMapsPermanentDenyToSettingsRequired() {
        assertEquals(
            SettingsRequired,
            classifyMediaNotificationPermission(
                facts(
                    sdkAtLeastTiramisu = true,
                    permissionGranted = false,
                    hasRequestedBefore = true,
                    shouldShowRationale = false,
                )),
        )
    }

    @Test
    fun notificationSettingsOpenOnlyForSettingsRequiredWhenResolvable() {
        assertTrue(
            shouldOpenNotificationSettings(
                SettingsRequired, settingsIntentResolvable = true))
        assertFalse(
            shouldOpenNotificationSettings(
                SettingsRequired,
                settingsIntentResolvable = false,
            ),
        )
        assertFalse(
            shouldOpenNotificationSettings(
                Requestable, settingsIntentResolvable = true),
        )
        assertFalse(
            shouldOpenNotificationSettings(
                Granted, settingsIntentResolvable = true),
        )
        assertFalse(
            shouldOpenNotificationSettings(
                Unavailable, settingsIntentResolvable = true),
        )
    }
}
