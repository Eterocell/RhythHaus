package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionState.Granted
import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionState.Requestable
import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionState.SettingsRequired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidMediaStoreAudioPermissionPolicyTest {

    private fun facts(
        permissionGranted: Boolean,
        hasRequestedBefore: Boolean,
        shouldShowRationale: Boolean,
    ) =
        MediaStoreAudioPermissionFacts(
            permissionGranted = permissionGranted,
            hasRequestedBefore = hasRequestedBefore,
            shouldShowRationale = shouldShowRationale,
        )

    @Test
    fun selectsReadMediaAudioOnApi33AndReadExternalStorageOnOlderApis() {
        assertEquals(
            MediaStoreAudioReadPermission.ReadExternalStorage,
            mediaStoreAudioReadPermissionForSdk(32),
        )
        assertEquals(
            MediaStoreAudioReadPermission.ReadMediaAudio,
            mediaStoreAudioReadPermissionForSdk(33),
        )
    }

    @Test
    fun grantedPermissionIsGrantedRegardlessOfRequestHistory() {
        assertEquals(
            Granted,
            classifyMediaStoreAudioPermission(
                facts(
                    permissionGranted = true,
                    hasRequestedBefore = false,
                    shouldShowRationale = false,
                ),
            ),
        )
        assertEquals(
            Granted,
            classifyMediaStoreAudioPermission(
                facts(
                    permissionGranted = true,
                    hasRequestedBefore = true,
                    shouldShowRationale = false,
                ),
            ),
        )
    }

    @Test
    fun firstDeniedStateIsRequestableForAnExplicitUserAction() {
        val neverRequested =
            facts(
                permissionGranted = false,
                hasRequestedBefore = false,
                shouldShowRationale = false,
            )

        assertEquals(
            Requestable, classifyMediaStoreAudioPermission(neverRequested))
        assertTrue(
            canRequestMediaStoreAudioPermission(
                neverRequested,
                requestInFlight = false,
            ),
        )
    }

    @Test
    fun rationaleEligibleDenialCanBeExplicitlyRequestedAgain() {
        val deniedWithRationale =
            facts(
                permissionGranted = false,
                hasRequestedBefore = true,
                shouldShowRationale = true,
            )

        assertEquals(
            Requestable,
            classifyMediaStoreAudioPermission(deniedWithRationale),
        )
        assertTrue(
            canRequestMediaStoreAudioPermission(
                deniedWithRationale,
                requestInFlight = false,
            ),
        )
    }

    @Test
    fun permanentDenialRequiresSettingsRecovery() {
        val permanentlyDenied =
            facts(
                permissionGranted = false,
                hasRequestedBefore = true,
                shouldShowRationale = false,
            )

        assertEquals(
            SettingsRequired,
            classifyMediaStoreAudioPermission(permanentlyDenied),
        )
        assertFalse(
            canRequestMediaStoreAudioPermission(
                permanentlyDenied,
                requestInFlight = false,
            ),
        )
        assertTrue(
            shouldOpenMediaStoreAudioSettings(
                permanentlyDenied,
                settingsIntentResolvable = true,
            ),
        )
        assertFalse(
            shouldOpenMediaStoreAudioSettings(
                permanentlyDenied,
                settingsIntentResolvable = false,
            ),
        )
    }

    @Test
    fun currentGrantedFactsSuppressAStaleRequestAction() {
        val currentlyGranted =
            facts(
                permissionGranted = true,
                hasRequestedBefore = true,
                shouldShowRationale = false,
            )

        assertFalse(
            canRequestMediaStoreAudioPermission(
                currentlyGranted,
                requestInFlight = false,
            ),
        )
        assertFalse(
            shouldOpenMediaStoreAudioSettings(
                currentlyGranted,
                settingsIntentResolvable = true,
            ),
        )
    }

    @Test
    fun requestActionIsSingleFlight() {
        val requestable =
            facts(
                permissionGranted = false,
                hasRequestedBefore = false,
                shouldShowRationale = false,
            )

        assertFalse(
            canRequestMediaStoreAudioPermission(
                requestable,
                requestInFlight = true,
            ),
        )
    }
}
