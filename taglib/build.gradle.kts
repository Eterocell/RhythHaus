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

val upstreamTagLibVersion = "v2.3"
val upstreamTagLibPinnedCommit = "1b94b93762636ebe5733180c3e825be4621e4c7f"
val upstreamTagLibSourceDirectory = layout.buildDirectory.dir("third-party/taglib-src-v2.3").get().asFile
val upstreamTagLibBuildDirectory = layout.buildDirectory.dir("third-party/taglib-build-v2.3").get().asFile
val upstreamTagLibInstallDirectory = layout.buildDirectory.dir("third-party/taglib-install-v2.3").get().asFile
val upstreamTagLibInstalledLibrary = upstreamTagLibInstallDirectory.resolve("lib/libtag.a")
val upstreamTagLibInstalledIncludeDirectory = upstreamTagLibInstallDirectory.resolve("include")

val checkoutUpstreamTagLib by tasks.registering(Exec::class) {
    outputs.dir(upstreamTagLibSourceDirectory)
    outputs.upToDateWhen { false }
    executable = "/bin/sh"
    args(
        "-c",
        """
        set -eu
        if [ ! -d '${upstreamTagLibSourceDirectory.absolutePath}/.git' ]; then
          mkdir -p '${upstreamTagLibSourceDirectory.parentFile.absolutePath}'
          git clone --depth 1 --branch '$upstreamTagLibVersion' https://github.com/taglib/taglib.git '${upstreamTagLibSourceDirectory.absolutePath}'
        fi
        git -C '${upstreamTagLibSourceDirectory.absolutePath}' checkout --detach '$upstreamTagLibPinnedCommit'
        git -C '${upstreamTagLibSourceDirectory.absolutePath}' submodule update --init --depth 1
        actual_commit="$(git -C '${upstreamTagLibSourceDirectory.absolutePath}' rev-parse HEAD)"
        if [ "${'$'}actual_commit" != '$upstreamTagLibPinnedCommit' ]; then
          echo "Expected TagLib commit $upstreamTagLibPinnedCommit but found ${'$'}actual_commit" >&2
          exit 1
        fi
        """.trimIndent(),
    )
}

val configureUpstreamTagLib by tasks.registering(Exec::class) {
    dependsOn(checkoutUpstreamTagLib)
    inputs.dir(upstreamTagLibSourceDirectory)
    outputs.file(upstreamTagLibBuildDirectory.resolve("CMakeCache.txt"))
    executable = "cmake"
    args(
        "-S", upstreamTagLibSourceDirectory.absolutePath,
        "-B", upstreamTagLibBuildDirectory.absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        "-DCMAKE_INSTALL_PREFIX=${upstreamTagLibInstallDirectory.absolutePath}",
        "-DBUILD_SHARED_LIBS=OFF",
        "-DBUILD_TESTING=OFF",
        "-DBUILD_EXAMPLES=OFF",
        "-DBUILD_BINDINGS=OFF",
        "-DWITH_ZLIB=OFF",
    )
}

val installUpstreamTagLib by tasks.registering(Exec::class) {
    dependsOn(configureUpstreamTagLib)
    inputs.file(upstreamTagLibBuildDirectory.resolve("CMakeCache.txt"))
    outputs.file(upstreamTagLibInstalledLibrary)
    outputs.dir(upstreamTagLibInstalledIncludeDirectory)
    executable = "cmake"
    args(
        "--build", upstreamTagLibBuildDirectory.absolutePath,
        "--target", "install",
        "--config", "Release",
        "--parallel",
    )
}

val buildMacosTagLibHelper by tasks.registering(Exec::class) {
    dependsOn(installUpstreamTagLib)
    inputs.file(tagLibShimSourceFile)
    inputs.file(tagLibJniSourceFile)
    inputs.dir(tagLibIncludeDirectory)
    inputs.dir(upstreamTagLibInstalledIncludeDirectory)
    inputs.file(upstreamTagLibInstalledLibrary)
    outputs.file(macosTagLibHelperOutputFile)
    macosTagLibHelperOutputFile.parentFile.mkdirs()
    executable = "clang++"
    args(
        "-dynamiclib",
        "-std=c++17",
        "-DRH_TAGLIB_HAS_TAGLIB=1",
        "-I${tagLibIncludeDirectory.absolutePath}",
        "-I${upstreamTagLibInstalledIncludeDirectory.absolutePath}",
        "-I$javaHomePath/include",
        "-I$javaHomePath/include/darwin",
        tagLibShimSourceFile.absolutePath,
        tagLibJniSourceFile.absolutePath,
        upstreamTagLibInstalledLibrary.absolutePath,
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

        // Android native TagLib packaging is intentionally not enabled yet. The follow-up must build
        // the same pinned upstream https://github.com/taglib/taglib source used by JVM/macOS
        // (upstreamTagLibVersion=v2.3, upstreamTagLibPinnedCommit above), not a replacement parser
        // or unrelated native library. Expected build layout: one Android NDK/CMake build directory
        // per ABI under build/third-party/taglib-android-<abi>-build-v2.3, matching install outputs
        // under build/third-party/taglib-android-<abi>-install-v2.3, then link those libtag.a files
        // with native/src/rh_taglib.cpp and native/jni/rh_taglib_jni.cpp into packaged
        // librhythhaus_taglib.so slices for the supported ABIs.
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )
    // iOS native TagLib packaging is intentionally not enabled yet. The follow-up must build the
    // same pinned upstream https://github.com/taglib/taglib source used by JVM/macOS
    // (upstreamTagLibVersion=v2.3, upstreamTagLibPinnedCommit above), not a replacement parser or
    // unrelated native library. This checkout has the RhythHaus C ABI header at
    // native/include/rh_taglib.h but does not contain upstream TagLib v2.3 iOS static libraries or
    // an XCFramework, so no src/nativeInterop/cinterop/*.def file is committed yet. Expected layout
    // before adding a cinterop def and Gradle wiring:
    //   taglib/native/include/rh_taglib.h
    //   taglib/native/src/rh_taglib.cpp
    //   taglib/build/third-party/taglib-ios-device-build-v2.3 -> static libtag.a from upstream v2.3
    //   taglib/build/third-party/taglib-ios-simulator-build-v2.3 -> static libtag.a from upstream v2.3
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
