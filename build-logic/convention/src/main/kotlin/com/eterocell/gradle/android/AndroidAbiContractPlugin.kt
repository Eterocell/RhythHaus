package com.eterocell.gradle.android

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class AndroidAbiContractPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<RhythHausAndroidAbiContractExtension>(
            "rhythHausAndroidAbiContract",
        )
        extension.abis.convention(
            providers.gradleProperty(RHYTHHAUS_ANDROID_ABIS_PROPERTY)
                .map(::parseRhythHausAndroidAbis)
                .orElse(providers.provider { parseRhythHausAndroidAbis(null) }),
        )
        extension.splitApkEnabled.convention(
            providers.gradleProperty(RHYTHHAUS_ANDROID_SPLIT_APK_PROPERTY)
                .map(::isRhythHausSplitApkEnabled)
                .orElse(false),
        )
        extension.abis.finalizeValueOnRead()
        extension.splitApkEnabled.finalizeValueOnRead()
    }
}
