package com.eterocell.gradle.android

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

const val RHYTHHAUS_ANDROID_ABIS_PROPERTY = "rhythhaus.android.abis"
const val RHYTHHAUS_ANDROID_SPLIT_APK_PROPERTY = "rhythhaus.android.splitApk"

val APPROVED_RHYTHHAUS_ANDROID_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64")

abstract class RhythHausAndroidAbiContractExtension @Inject constructor(
    objects: ObjectFactory,
) {
    val abis: ListProperty<String> = objects.listProperty(String::class.java)
    val splitApkEnabled: Property<Boolean> = objects.property(Boolean::class.java)
}

fun parseRhythHausAndroidAbis(value: String?): List<String> {
    if (value.isNullOrBlank()) {
        throw IllegalArgumentException(
            "Missing required Gradle property '$RHYTHHAUS_ANDROID_ABIS_PROPERTY'; " +
                "expected '${APPROVED_RHYTHHAUS_ANDROID_ABIS.joinToString(",")}'.",
        )
    }

    val abis = value.split(',').map(String::trim)
    if (abis.any(String::isEmpty)) {
        throw IllegalArgumentException(
            "Gradle property '$RHYTHHAUS_ANDROID_ABIS_PROPERTY' contains a blank ABI entry.",
        )
    }

    val duplicates = abis.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    if (duplicates.isNotEmpty()) {
        throw IllegalArgumentException(
            "Gradle property '$RHYTHHAUS_ANDROID_ABIS_PROPERTY' contains duplicate ABI(s): " +
                duplicates.joinToString(", "),
        )
    }

    if (abis != APPROVED_RHYTHHAUS_ANDROID_ABIS) {
        throw IllegalArgumentException(
            "Gradle property '$RHYTHHAUS_ANDROID_ABIS_PROPERTY' must resolve to exactly " +
                "$APPROVED_RHYTHHAUS_ANDROID_ABIS in this order; actual $abis.",
        )
    }

    return abis
}

fun isRhythHausSplitApkEnabled(value: String?): Boolean = value == "true"

fun shouldConfigureSplitApks(
    splitRequested: Boolean,
    requestedTasks: List<String>,
): Boolean {
    if (!splitRequested) return false
    val taskNames = requestedTasks.map { it.substringAfterLast(':') }
    val requestsAab = taskNames.any { it == "bundleRelease" || it == "verifyReleaseAab" }
    val requestsApks = taskNames.any { it == "assembleRelease" || it == "verifyReleaseApks" }
    require(!(requestsAab && requestsApks)) {
        "AGP 9.3 requires split APK and AAB release tasks to run in separate Gradle invocations."
    }
    return !requestsAab
}
