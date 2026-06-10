import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.eterocell.rhythhaus.MainKt"

        nativeDistributions {
            // Product scope for now: Android, iOS, and macOS. Keep Windows/Linux packaging for later.
            targetFormats(TargetFormat.Dmg)
            packageName = "RhythHaus"
            packageVersion = "1.0.0"
        }
    }
}