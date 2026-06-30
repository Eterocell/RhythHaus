import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

val rhythHausVersionName = providers.gradleProperty("rhythhaus.versionName")
    .orElse(provider { throw GradleException("Missing required Gradle property 'rhythhaus.versionName'") })

compose.desktop {
    application {
        mainClass = "com.eterocell.rhythhaus.MainKt"

        nativeDistributions {
            // Product scope for now: Android, iOS, and macOS. Keep Windows/Linux packaging for later.
            targetFormats(TargetFormat.Dmg)
            packageName = "RhythHaus"
            packageVersion = rhythHausVersionName.get()
        }
    }
}
