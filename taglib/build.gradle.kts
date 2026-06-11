import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val nativeTagLibResourceRoot = layout.buildDirectory.dir("generated/nativeTagLibResources/jvmMain")
val macosTagLibResourceArch = when (System.getProperty("os.arch").lowercase()) {
    "aarch64", "arm64" -> "macos-aarch64"
    else -> "macos-x64"
}
val macosTagLibHelperOutputFile = layout.buildDirectory.file("generated/nativeTagLibResources/jvmMain/native/$macosTagLibResourceArch/librhythhaus_taglib.dylib").get().asFile
val tagLibShimSourceFile = layout.projectDirectory.file("native/src/rh_taglib.cpp").asFile
val tagLibJniSourceFile = layout.projectDirectory.file("native/jni/rh_taglib_jni.cpp").asFile
val tagLibIncludeDirectory = layout.projectDirectory.dir("native/include").asFile
val javaHomePath = providers.systemProperty("java.home").get()
val buildMacosTagLibHelper by tasks.registering(Exec::class) {
    inputs.file(tagLibShimSourceFile)
    inputs.file(tagLibJniSourceFile)
    inputs.dir(tagLibIncludeDirectory)
    outputs.file(macosTagLibHelperOutputFile)
    macosTagLibHelperOutputFile.parentFile.mkdirs()
    executable = "clang++"
    args(
        "-dynamiclib",
        "-std=c++17",
        "-I${tagLibIncludeDirectory.absolutePath}",
        "-I$javaHomePath/include",
        "-I$javaHomePath/include/darwin",
        tagLibShimSourceFile.absolutePath,
        tagLibJniSourceFile.absolutePath,
        "-o", macosTagLibHelperOutputFile.absolutePath,
    )
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    jvm()

    android {
        namespace = "com.eterocell.rhythhaus.taglib"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Android native TagLib packaging is intentionally not enabled yet: this checkout does not
        // contain TagLib Android source or ABI prebuilts. When those inputs are added, wire this
        // target to an Android NDK/CMake build that compiles native/src/rh_taglib.cpp together with
        // native/jni/rh_taglib_jni.cpp and links TagLib for each supported ABI.
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )
    // iOS native TagLib packaging is intentionally not enabled yet: this checkout has the
    // RhythHaus C ABI header at native/include/rh_taglib.h but does not contain a TagLib
    // iOS static library or XCFramework, so no src/nativeInterop/cinterop/*.def file is
    // committed yet. Expected layout before adding a cinterop def and Gradle wiring:
    //   taglib/native/include/rh_taglib.h
    //   taglib/native/src/rh_taglib.cpp
    //   taglib/third_party/taglib-ios/TagLib.xcframework
    //     ios-arm64/... device library/framework slice with TagLib headers
    //     ios-arm64_x86_64-simulator/... simulator library/framework slice with TagLib headers
    // Once those inputs are added, wire per-target cinterops here and keep iosMain returning
    // Unsupported until that link is verified.

    sourceSets {
        jvmMain {
            resources.srcDir(nativeTagLibResourceRoot)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.matching { it.name in setOf("jvmProcessResources", "processJvmMainResources") }.configureEach {
    dependsOn(buildMacosTagLibHelper)
}
