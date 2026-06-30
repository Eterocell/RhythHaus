package com.eterocell.gradle.dsl

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

fun Project.withAndroidApplication(block: Plugin<in Any>.() -> Unit) =
    plugins.withId("com.android.application", block)

fun Project.withAndroidLibrary(block: Plugin<in Any>.() -> Unit) =
    plugins.withId("com.android.library", block)

fun Project.withAndroidDynamicFeature(block: Plugin<in Any>.() -> Unit) =
    plugins.withId("com.android.dynamic-feature", block)

fun Project.withAndroid(block: Plugin<in Any>.() -> Unit) {
    withAndroidApplication(block)
    withAndroidLibrary(block)
    withAndroidDynamicFeature(block)
}

fun Project.configureAndroidCommon(action: Action<CommonExtension>) {
    withAndroidApplication {
        action.execute(extensions.getByType<ApplicationExtension>())
    }
    withAndroidLibrary {
        action.execute(extensions.getByType<LibraryExtension>())
    }
    withAndroidDynamicFeature {
        action.execute(extensions.getByType<DynamicFeatureExtension>())
    }
}

fun Project.configureAndroidApplication(block: ApplicationExtension.() -> Unit) =
    withAndroidApplication { extensions.configure(block) }

fun Project.configureAndroidLibrary(block: LibraryExtension.() -> Unit) =
    withAndroidLibrary { extensions.configure(block) }

fun Project.configureAndroidDynamicFeatures(block: DynamicFeatureExtension.() -> Unit) =
    withAndroidDynamicFeature { extensions.configure(block) }

fun Project.withBuildType(
    buildType: String,
    block: () -> Unit,
) {
    if (taskRequestContains(buildType)) block()
}
