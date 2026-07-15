package com.eterocell.gradle.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AndroidAbiContractTest {
    @Test
    fun approvedOrderedValueIsAccepted() {
        assertEquals(
            APPROVED_RHYTHHAUS_ANDROID_ABIS,
            parseRhythHausAndroidAbis("arm64-v8a,armeabi-v7a,x86_64"),
        )
    }

    @Test
    fun acceptedEntriesAreTrimmed() {
        assertEquals(
            APPROVED_RHYTHHAUS_ANDROID_ABIS,
            parseRhythHausAndroidAbis(" arm64-v8a , armeabi-v7a , x86_64 "),
        )
    }

    @Test
    fun missingAndBlankValuesAreRejected() {
        listOf(null, "", "   ").forEach { value ->
            val error = assertFailsWith<IllegalArgumentException> {
                parseRhythHausAndroidAbis(value)
            }

            assertTrue(
                error.message.orEmpty().contains(
                    "Missing required Gradle property 'rhythhaus.android.abis'; expected 'arm64-v8a,armeabi-v7a,x86_64'.",
                ),
            )
        }
    }

    @Test
    fun blankEntriesAreRejected() {
        val error = assertFailsWith<IllegalArgumentException> {
            parseRhythHausAndroidAbis("arm64-v8a,,x86_64")
        }

        assertTrue(error.message.orEmpty().contains("contains a blank ABI entry"))
    }

    @Test
    fun duplicateEntriesAreRejected() {
        val error = assertFailsWith<IllegalArgumentException> {
            parseRhythHausAndroidAbis("arm64-v8a,arm64-v8a,x86_64")
        }

        assertTrue(error.message.orEmpty().contains("contains duplicate ABI(s): arm64-v8a"))
    }

    @Test
    fun reorderedEntriesAreRejected() {
        assertExactApprovedOrderError("x86_64,armeabi-v7a,arm64-v8a")
    }

    @Test
    fun unsupportedAbiIsRejected() {
        assertExactApprovedOrderError("arm64-v8a,armeabi-v7a,x86")
    }

    @Test
    fun reducedAbiSetIsRejected() {
        assertExactApprovedOrderError("arm64-v8a,x86_64")
    }

    @Test
    fun splitModeRequiresExactLowercaseTrue() {
        assertTrue(isRhythHausSplitApkEnabled("true"))
        listOf(null, "", "TRUE", "True", " true", "true ", "1", "yes").forEach { value ->
            assertFalse(isRhythHausSplitApkEnabled(value))
        }
    }

    private fun assertExactApprovedOrderError(value: String) {
        val error = assertFailsWith<IllegalArgumentException> {
            parseRhythHausAndroidAbis(value)
        }

        assertTrue(
            error.message.orEmpty().contains(
                "must resolve to exactly [arm64-v8a, armeabi-v7a, x86_64] in this order",
            ),
        )
    }
}
