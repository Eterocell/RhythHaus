package com.eterocell.rhythhaus.notificationpermission

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaNotificationPermissionControllerTest {

    @Test
    fun stateValuesAreExactlyTheSharedContract() {
        assertEquals(
            listOf(
                MediaNotificationPermissionState.Unavailable,
                MediaNotificationPermissionState.Granted,
                MediaNotificationPermissionState.Requestable,
                MediaNotificationPermissionState.SettingsRequired,
            ),
            MediaNotificationPermissionState.entries,
        )
    }

    @Test
    fun defaultControllerExposesUnavailableState() {
        assertEquals(
            MediaNotificationPermissionState.Unavailable,
            UnavailableMediaNotificationPermissionController.state.value,
        )
    }

    @Test
    fun defaultControllerActionsAreNoOpsThatKeepUnavailableState() {
        val controller = UnavailableMediaNotificationPermissionController
        controller.requestPermission()
        controller.openAppNotificationSettings()
        assertEquals(
            MediaNotificationPermissionState.Unavailable,
            controller.state.value,
        )
    }
}
