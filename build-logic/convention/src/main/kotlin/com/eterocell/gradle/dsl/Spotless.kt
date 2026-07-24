package com.eterocell.gradle.dsl

import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import com.diffplug.spotless.kotlin.KtLintStep
import com.diffplug.spotless.kotlin.KtfmtStep
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import java.io.File

fun Project.configureSpotless(block: SpotlessExtension.() -> Unit) {
    apply<SpotlessPlugin>()
    plugins.withId("com.diffplug.spotless") { configure(block) }
}

fun SpotlessExtension.androidXml(
    block: FormatExtension.() -> Unit = {
        target(
            "**/AndroidManifest.xml",
            "src/**/*.xml",
        )
        leadingTabsToSpaces()
        trimTrailingWhitespace()
        endWithNewline()
    },
) = format("androidXml", block)

fun SpotlessExtension.gradleVersionCatalogs(
    block: FormatExtension.() -> Unit = {
        target(
            "**/*.versions.toml",
        )
        leadingTabsToSpaces()
        trimTrailingWhitespace()
        endWithNewline()
    },
) = format("gradleVersionCatalogs", block)

fun SpotlessExtension.intelliJIDEARunConfiguration(
    block: FormatExtension.() -> Unit = {
        target(
            "**/.run/*.xml",
            "**/.idea/runConfigurations/*.xml",
        )
        leadingTabsToSpaces()
        trimTrailingWhitespace()
        endWithNewline()
    },
) = format("intelliJIDEARunConfiguration", block)

fun SpotlessExtension.kotlin(
    targets: List<String> = listOf("src/**/*.kt"),
    excludeTargets: List<String> = listOf(),
    ktfmtVersion: String = KtfmtStep.defaultVersion(),
    ktfmtConfig: KtfmtStep.KtfmtFormattingOptions.() -> Unit = {},
    licenseHeaderFile: File? = null,
    licenseHeaderConfig: FormatExtension.LicenseHeaderConfig.() -> Unit = {},
) = kotlin {
    target(targets)
    targetExclude(excludeTargets)
    leadingTabsToSpaces()
    trimTrailingWhitespace()
    endWithNewline()
    ktfmt(ktfmtVersion).googleStyle().configure(ktfmtConfig)
    licenseHeaderFile?.let(::licenseHeaderFile)?.apply(licenseHeaderConfig)
}

private val defaultExcludeTargetsForKotlinGradle: Set<String> = setOf(
    "**/build/kotlin-dsl/**/*.gradle.kts",
)

fun SpotlessExtension.kotlinGradle(
    targets: List<String> = listOf("**/*.gradle.kts"),
    overrideExcludeTargets: Set<String> = setOf(),
    additionalExcludeTargets: Set<String> = setOf(),
    ktfmtVersion: String = KtfmtStep.defaultVersion(),
    ktfmtConfig: KtfmtStep.KtfmtFormattingOptions.() -> Unit = {},
) = kotlinGradle {
    target(targets)
    targetExclude(
        overrideExcludeTargets.ifEmpty {
            defaultExcludeTargetsForKotlinGradle + additionalExcludeTargets
        },
    )
    leadingTabsToSpaces()
    trimTrailingWhitespace()
    endWithNewline()
    ktfmt(ktfmtVersion).googleStyle().configure(ktfmtConfig)
}

fun SpotlessExtension.cAndCpp(
    clangFormatVersion: String = "*",
    style: String = "Google",
    block: FormatExtension.() -> Unit = {
        target("src/**/*.c", "src/**/*.cpp", "src/**/*.cc", "src/**/*.h", "src/**/*.hpp")
        clangFormat(clangFormatVersion).style(style)
    },
) = format("c", block)

fun SpotlessExtension.protobuf(
    clangFormatVersion: String = "*",
    style: String = "Google",
    block: FormatExtension.() -> Unit = {
        target("src/**/*.proto")
        clangFormat(clangFormatVersion).style(style)
    },
) = format("protobuf", block)

fun SpotlessExtension.copyrightForKts(
    targets: List<String> = listOf("**/*.kts"),
    excludeTargets: Set<String> = setOf(),
    licenseHeaderFile: File? = null,
    licenseHeaderDelimiter: String = "(^(?![\\/ ]\\*).*\$)",
    licenseHeaderConfig: FormatExtension.LicenseHeaderConfig.() -> Unit = {},
) {
    format("kts") {
        target(targets)
        targetExclude(excludeTargets)
        licenseHeaderFile?.let { licenseHeaderFile(it, licenseHeaderDelimiter) }
            ?.apply(licenseHeaderConfig)
    }
}

fun SpotlessExtension.copyrightForXml(
    targets: List<String> = listOf("**/*.xml"),
    excludeTargets: Set<String> = setOf(),
    licenseHeaderFile: File? = null,
    licenseHeaderDelimiter: String = "(<[^!?])",
    licenseHeaderConfig: FormatExtension.LicenseHeaderConfig.() -> Unit = {},
) {
    format("xml") {
        target(targets)
        targetExclude(excludeTargets)
        licenseHeaderFile?.let { licenseHeaderFile(it, licenseHeaderDelimiter) }
            ?.apply(licenseHeaderConfig)
    }
}
