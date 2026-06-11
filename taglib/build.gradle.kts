import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val nativeTagLibResourceRoot = layout.buildDirectory.dir("generated/nativeTagLibResources/jvmMain")
val macosTagLibResourceArch = when (System.getProperty("os.arch").lowercase()) {
    "aarch64", "arm64" -> "macos-aarch64"
    else -> "macos-x64"
}
val macosCmakeArch = when (System.getProperty("os.arch").lowercase()) {
    "aarch64", "arm64" -> "arm64"
    else -> "x86_64"
}
val macosTagLibHelperOutputFile = layout.buildDirectory.file("generated/nativeTagLibResources/jvmMain/native/$macosTagLibResourceArch/librhythhaus_taglib.dylib").get().asFile
val nativeTagLibDirectory = layout.projectDirectory.dir("native").asFile
val macosTagLibHelperBuildDirectory = layout.buildDirectory.dir("native/macosTagLibHelper-$macosCmakeArch").get().asFile
val macosTagLibHelperBuiltFile = macosTagLibHelperBuildDirectory.resolve("librhythhaus_taglib.dylib")
val javaHomePath = providers.systemProperty("java.home").get()

val buildMacosTagLibHelper by tasks.registering(Exec::class) {
    inputs.dir(nativeTagLibDirectory)
    outputs.file(macosTagLibHelperOutputFile)
    macosTagLibHelperOutputFile.parentFile.mkdirs()
    environment("JAVA_HOME", javaHomePath)
    executable = "/bin/sh"
    args(
        "-c",
        """
        set -eu
        cmake -S '${nativeTagLibDirectory.absolutePath}' -B '${macosTagLibHelperBuildDirectory.absolutePath}' \
          -DCMAKE_BUILD_TYPE=Release \
          -DCMAKE_OSX_ARCHITECTURES='$macosCmakeArch' \
          -DCMAKE_LIBRARY_OUTPUT_DIRECTORY='${macosTagLibHelperBuildDirectory.absolutePath}' \
          -DCMAKE_EXPORT_COMPILE_COMMANDS=ON \
          -DRHYTHHAUS_TAGLIB_BUILD_JNI=ON
        cmake --build '${macosTagLibHelperBuildDirectory.absolutePath}' --target rhythhaus_taglib --config Release --parallel
        cmake -E make_directory '${macosTagLibHelperOutputFile.parentFile.absolutePath}'
        cmake -E copy_if_different '${macosTagLibHelperBuiltFile.absolutePath}' '${macosTagLibHelperOutputFile.absolutePath}'
        """.trimIndent(),
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

        // Android native TagLib packaging is intentionally not enabled yet. The follow-up should use
        // native/CMake FetchContent for the same pinned upstream https://github.com/taglib/taglib
        // source used by JVM/macOS (v2.3 commit 1b94b93762636ebe5733180c3e825be4621e4c7f), not a
        // replacement parser or unrelated native library. Expected build layout: one Android
        // NDK/CMake build directory per ABI, then package librhythhaus_taglib.so slices for the
        // supported ABIs from native/src/rh_taglib.cpp and native/jni/rh_taglib_jni.cpp.
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )
    // iOS native TagLib packaging is intentionally not enabled yet. The follow-up must build the
    // same native/CMake FetchContent-pinned upstream https://github.com/taglib/taglib source used by
    // JVM/macOS (v2.3 commit 1b94b93762636ebe5733180c3e825be4621e4c7f), not a replacement parser or
    // unrelated native library. This checkout has the RhythHaus C ABI header at
    // native/include/rh_taglib.h but does not contain upstream TagLib v2.3 iOS static libraries or
    // an XCFramework, so no src/nativeInterop/cinterop/*.def file is committed yet. Expected layout
    // before adding a cinterop def and Gradle wiring:
    //   taglib/native/include/rh_taglib.h
    //   taglib/native/src/rh_taglib.cpp
    //   taglib/build/native/taglib-ios-device-build-v2.3 -> static libtag.a from upstream v2.3
    //   taglib/build/native/taglib-ios-simulator-build-v2.3 -> static libtag.a from upstream v2.3
    //   taglib/third_party/taglib-ios/TagLib.xcframework
    //     ios-arm64/... device static library/framework slice with upstream TagLib headers
    //     ios-arm64_x86_64-simulator/... simulator static library/framework slice with upstream TagLib headers
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
