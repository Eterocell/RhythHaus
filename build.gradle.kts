import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.build.logic.root.project)

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false

    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.version.catalog.update)
}

val rhythHausVersionName =
    findProperty("rhythhaus.versionName")?.toString()
        ?: throw GradleException(
            "Missing required Gradle property 'rhythhaus.versionName'")
val rhythHausVersionCode =
    findProperty("rhythhaus.versionCode")?.toString()
        ?: throw GradleException(
            "Missing required Gradle property 'rhythhaus.versionCode'")

@CacheableTask
abstract class SyncIosVersionXcconfigTask : DefaultTask() {
    @get:Input abstract val versionName: Property<String>

    @get:Input abstract val versionCode: Property<String>

    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun writeXcconfig() {
        val code = versionCode.get()
        code.toIntOrNull()
            ?: throw GradleException(
                "Gradle property 'rhythhaus.versionCode' must be an integer, was '$code'")
        outputFile
            .get()
            .asFile
            .writeText(
                """
            // Generated from root gradle.properties by ./gradlew syncIosVersionXcconfig.
            // Edit rhythhaus.versionName and rhythhaus.versionCode in ../../gradle.properties.
            RHYTHHAUS_VERSION_NAME = ${versionName.get()}
            RHYTHHAUS_VERSION_CODE = $code
            """
                    .trimIndent() + "\n",
            )
    }
}

tasks.register<SyncIosVersionXcconfigTask>("syncIosVersionXcconfig") {
    versionName.set(rhythHausVersionName)
    versionCode.set(rhythHausVersionCode)
    outputFile.set(
        layout.projectDirectory.file("iosApp/Configuration/Version.xcconfig"))
}

fun isNonStable(version: String): Boolean {
    val stableKeyword =
        listOf("RELEASE", "FINAL", "GA").any {
            version.uppercase().contains(it)
        }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    val nonStableModuleAllowList = listOf("androidx.security:security-state")
    val nonStableGroupAllowList =
        listOf(
            "com.squareup.okhttp3",
            "androidx.biometric",
            "androidx.camera",
            "androidx.metrics",
        )

    rejectVersionIf {
        if (candidate.moduleIdentifier.toString() == "com.google.guava:guava") {
            return@rejectVersionIf "-jre" in candidate.version
        }
        !nonStableGroupAllowList.contains(candidate.group) &&
            !nonStableModuleAllowList.contains(candidate.module) &&
            isNonStable(candidate.version)
    }
}

versionCatalogUpdate {
    sortByKey = true
    keep {
        keepUnusedVersions = true
    }
}
