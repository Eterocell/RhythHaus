import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.eterocell.gradle.android.RhythHausAndroidAbiContractExtension
import com.eterocell.gradle.android.VerifyReleaseAabTask
import com.eterocell.gradle.android.VerifyReleaseApksTask
import com.eterocell.gradle.android.resolveAapt2
import com.eterocell.gradle.android.resolveApkAnalyzer
import com.eterocell.gradle.android.resolveApkSigner
import com.eterocell.gradle.android.shouldConfigureSplitApks
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.build.logic.android.application)
    id("build-logic.android.abi-contract")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

val rhythHausVersionName = providers.gradleProperty("rhythhaus.versionName")
    .orElse(provider { throw GradleException("Missing required Gradle property 'rhythhaus.versionName'") })
val rhythHausVersionCode = providers.gradleProperty("rhythhaus.versionCode")
    .orElse(provider { throw GradleException("Missing required Gradle property 'rhythhaus.versionCode'") })
val androidAbiContract = extensions.getByType<RhythHausAndroidAbiContractExtension>()
val splitApkEnabled = shouldConfigureSplitApks(
    androidAbiContract.splitApkEnabled.get(),
    gradle.startParameter.taskNames,
)
val supportedAndroidAbis = androidAbiContract.abis.get()

android {
    namespace = "com.eterocell.rhythhaus"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.eterocell.rhythhaus"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = rhythHausVersionCode.get().toIntOrNull()
            ?: throw GradleException("Gradle property 'rhythhaus.versionCode' must be an integer, was '${rhythHausVersionCode.get()}'")
        versionName = rhythHausVersionName.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    splits {
        abi {
            isEnable = splitApkEnabled
            if (splitApkEnabled) {
                reset()
                include(*supportedAndroidAbis.toTypedArray())
                isUniversalApk = true
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

val androidExtension = extensions.getByType<ApplicationExtension>()
val androidComponents = extensions.getByType<ApplicationAndroidComponentsExtension>()
val androidSdkDirectory = androidComponents.sdkComponents.sdkDirectory
val resolvedAapt2 = androidSdkDirectory.map {
    resolveAapt2(it.asFile, androidExtension.buildToolsVersion)
}
val resolvedApkAnalyzer = androidSdkDirectory.map { resolveApkAnalyzer(it.asFile) }
val resolvedApkSigner = androidSdkDirectory.map {
    resolveApkSigner(it.asFile, androidExtension.buildToolsVersion)
}.filter { it != null }.map { it!! }
androidComponents.onVariants(androidComponents.selector().withBuildType("release")) { variant ->
    tasks.register<VerifyReleaseApksTask>("verifyReleaseApks") {
        apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
        builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
        supportedAbis.set(androidAbiContract.abis)
        splitApkEnabled.set(androidAbiContract.splitApkEnabled)
        expectedApplicationId.set("com.eterocell.rhythhaus")
        expectedVersionName.set(rhythHausVersionName)
        expectedVersionCode.set(
            rhythHausVersionCode.map {
                it.toIntOrNull()
                    ?: throw GradleException(
                        "Gradle property 'rhythhaus.versionCode' must be an integer, was '$it'",
                    )
            },
        )
        releaseSigningConfigured.set(androidExtension.buildTypes.getByName("release").signingConfig != null)
        apkAnalyzerExecutable.set(layout.file(resolvedApkAnalyzer))
        apkSignerExecutable.set(layout.file(resolvedApkSigner))
        reportFile.set(layout.buildDirectory.file("reports/androidReleaseVerification/release-apks.txt"))
    }
    tasks.register<VerifyReleaseAabTask>("verifyReleaseAab") {
        aabFile.set(variant.artifacts.get(SingleArtifact.BUNDLE))
        expectedApplicationId.set("com.eterocell.rhythhaus")
        expectedVersionName.set(rhythHausVersionName)
        expectedVersionCode.set(
            rhythHausVersionCode.map {
                it.toIntOrNull()
                    ?: throw GradleException(
                        "Gradle property 'rhythhaus.versionCode' must be an integer, was '$it'",
                    )
            },
        )
        aapt2Executable.set(layout.file(resolvedAapt2))
        apkAnalyzerExecutable.set(layout.file(resolvedApkAnalyzer))
        reportFile.set(layout.buildDirectory.file("reports/androidReleaseVerification/release-aab.txt"))
    }
}
