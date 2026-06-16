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

// Android NDK native TagLib builds per ABI

val androidNdkVersion = System.getenv("ANDROID_NDK_VERSION") ?: "30.0.14904198"
val androidHome = System.getenv("ANDROID_HOME") ?: "${System.getProperty("user.home")}/Library/Android/sdk"
val androidNdkDir = file("$androidHome/ndk/$androidNdkVersion")
val androidTagLibToolchainFile = file("$androidNdkDir/build/cmake/android.toolchain.cmake")

val androidTagLibAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")
val androidTagLibMinSdk = libs.versions.android.minSdk.get().toInt()

val androidTagLibOutputRoot = layout.buildDirectory.dir("generated/androidNativeLibs").get().asFile

val androidNativeBuildTasks = androidTagLibAbis.map { abi ->
    val ndkAbi = when (abi) {
        "arm64-v8a" -> "aarch64-linux-android"
        "armeabi-v7a" -> "armv7a-linux-androideabi"
        "x86_64" -> "x86_64-linux-android"
        else -> error("unsupported ABI: $abi")
    }
    val ndkToolchain = "${ndkAbi}${androidTagLibMinSdk}"
    val buildDir = layout.buildDirectory.dir("native/androidTagLibHelper-$abi").get().asFile
    val outputSo = androidTagLibOutputRoot.resolve("$abi/librhythhaus_taglib.so")

    abi to tasks.register("buildAndroidTagLibHelper-$abi", Exec::class) {
        inputs.dir(nativeTagLibDirectory)
        outputs.file(outputSo)
        doFirst {
            outputSo.parentFile.mkdirs()
        }
        executable = "/bin/sh"
        args(
            "-c",
            """
            set -eu
            cmake -S '${nativeTagLibDirectory.absolutePath}' -B '${buildDir.absolutePath}' \
              -DCMAKE_BUILD_TYPE=Release \
              -DCMAKE_TOOLCHAIN_FILE='${androidTagLibToolchainFile.absolutePath}' \
              -DANDROID_ABI=$abi \
              -DANDROID_PLATFORM=android-$androidTagLibMinSdk \
              -DANDROID_STL=c++_static \
              -DCMAKE_LIBRARY_OUTPUT_DIRECTORY='${buildDir.absolutePath}' \
              -DCMAKE_EXPORT_COMPILE_COMMANDS=ON \
              -DRHYTHHAUS_TAGLIB_BUILD_JNI=ON
            cmake --build '${buildDir.absolutePath}' --target rhythhaus_taglib --config Release --parallel
            cmake -E copy_if_different '${buildDir.resolve("librhythhaus_taglib.so").absolutePath}' '${outputSo.absolutePath}'
            """.trimIndent(),
        )
    }
}.toMap()

val buildAllAndroidTagLibHelpers by tasks.registering {
    dependsOn(androidNativeBuildTasks.values)
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

        // Android native TagLib packaging uses the same native/CMake FetchContent-pinned upstream
        // github.com/taglib/taglib source as JVM/macOS (v2.3 commit 1b94b93762636ebe5733180c3e825be4621e4c7f).
        // Per-ABI .so slices are built by buildAndroidTagLibHelper-<abi> Exec tasks and output to
        // build/generated/androidNativeLibs/<abi>/librhythhaus_taglib.so.

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

// Wire Android native TagLib builds before AGP merges jniLibs.
// AGP automatically packages jniLibs from build/generated/androidNativeLibs/<abi>/.
tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn(buildAllAndroidTagLibHelpers)
}
tasks.matching { it.name.contains("copy") && it.name.contains("JniLibsProjectOnly") }.configureEach {
    dependsOn(buildAllAndroidTagLibHelpers)
}

// Point AGP at the build output directory for jniLibs instead of src/.
afterEvaluate {
    extensions.findByType<com.android.build.api.dsl.LibraryExtension>()?.sourceSets?.getByName("main") {
        jniLibs.srcDir(layout.buildDirectory.dir("generated/androidNativeLibs"))
    }
}
