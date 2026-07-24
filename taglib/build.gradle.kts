import com.eterocell.gradle.android.RhythHausAndroidAbiContractExtension
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("build-logic.android.abi-contract")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
}

val nativeTagLibResourceRoot =
    layout.buildDirectory.dir("generated/nativeTagLibResources/jvmMain")
val macosTagLibResourceArch =
    when (System.getProperty("os.arch").lowercase()) {
        "aarch64",
        "arm64" -> "macos-aarch64"
        else -> "macos-x64"
    }
val macosCmakeArch =
    when (System.getProperty("os.arch").lowercase()) {
        "aarch64",
        "arm64" -> "arm64"
        else -> "x86_64"
    }
val macosTagLibHelperOutputFile =
    layout.buildDirectory
        .file(
            "generated/nativeTagLibResources/jvmMain/native/$macosTagLibResourceArch/librhythhaus_taglib.dylib")
        .get()
        .asFile
val nativeTagLibDirectory = layout.projectDirectory.dir("native").asFile
val macosTagLibHelperBuildDirectory =
    layout.buildDirectory
        .dir("native/macosTagLibHelper-$macosCmakeArch")
        .get()
        .asFile
val macosTagLibHelperBuiltFile =
    macosTagLibHelperBuildDirectory.resolve("librhythhaus_taglib.dylib")
val javaHomePath = providers.systemProperty("java.home").get()

val buildMacosTagLibHelper by
    tasks.registering(Exec::class) {
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
        """
                .trimIndent(),
        )
    }

// iOS static TagLib builds for Kotlin/Native cinterop

data class IosTagLibBuild(
    val targetName: String,
    val sdk: String,
    val arch: String,
)

val iosTagLibDeploymentTarget =
    providers
        .environmentVariable("IPHONEOS_DEPLOYMENT_TARGET")
        .orElse(providers.gradleProperty("rhythhaus.ios.deploymentTarget"))

val iosTagLibBuilds =
    listOf(
        IosTagLibBuild(
            targetName = "iosArm64", sdk = "iphoneos", arch = "arm64"),
        IosTagLibBuild(
            targetName = "iosSimulatorArm64",
            sdk = "iphonesimulator",
            arch = "arm64"),
    )

val iosCinteropDirectory = project.file("src/nativeInterop/cinterop")

val iosTagLibStaticLibraries = iosTagLibBuilds.associate { build ->
    build.targetName to
        layout.buildDirectory
            .file(
                "generated/iosTagLib/${build.targetName}/librhythhaus_taglib.a")
            .get()
            .asFile
}

val iosTagLibUpstreamLibraries = iosTagLibBuilds.associate { build ->
    build.targetName to
        layout.buildDirectory
            .file("generated/iosTagLib/${build.targetName}/libtag.a")
            .get()
            .asFile
}

val iosNativeBuildTasks = iosTagLibBuilds.associate { build ->
    val buildDirectory =
        layout.buildDirectory
            .dir("native/taglib-${build.targetName}")
            .get()
            .asFile
    val outputLibrary = iosTagLibStaticLibraries.getValue(build.targetName)
    val upstreamLib = iosTagLibUpstreamLibraries.getValue(build.targetName)
    val taskName =
        "buildIosTagLibHelper${build.targetName.replaceFirstChar { it.uppercase() }}"
    val upstreamTagLibDir = buildDirectory.absolutePath
    build.targetName to
        tasks.register(taskName, Exec::class) {
            inputs.dir(nativeTagLibDirectory)
            inputs.property(
                "iosTagLibDeploymentTarget", iosTagLibDeploymentTarget)
            outputs.file(outputLibrary)
            outputs.file(upstreamLib)
            doFirst {
                outputLibrary.parentFile.mkdirs()
            }
            executable = "/bin/sh"
            args(
                "-c",
                """
            set -eu
            SDK_PATH=$(xcrun --sdk ${build.sdk} --show-sdk-path)
            cmake -S '${nativeTagLibDirectory.absolutePath}' -B '${buildDirectory.absolutePath}' \
              -DCMAKE_BUILD_TYPE=Release \
              -DCMAKE_SYSTEM_NAME=iOS \
              -DCMAKE_OSX_SYSROOT="${'$'}SDK_PATH" \
              -DCMAKE_OSX_ARCHITECTURES='${build.arch}' \
              -DCMAKE_OSX_DEPLOYMENT_TARGET='${iosTagLibDeploymentTarget.get()}' \
              -DCMAKE_XCODE_ATTRIBUTE_ONLY_ACTIVE_ARCH=NO \
              -DCMAKE_LIBRARY_OUTPUT_DIRECTORY='${buildDirectory.absolutePath}' \
              -DCMAKE_ARCHIVE_OUTPUT_DIRECTORY='${buildDirectory.absolutePath}' \
              -DCMAKE_EXPORT_COMPILE_COMMANDS=ON \
              -DRHYTHHAUS_TAGLIB_BUILD_STATIC=ON
            cmake --build '${buildDirectory.absolutePath}' --target rhythhaus_taglib --config Release --parallel
            cmake -E copy_if_different '${buildDirectory.resolve("librhythhaus_taglib.a").absolutePath}' '${outputLibrary.absolutePath}'
            cmake -E copy_if_different '$upstreamTagLibDir/libtag.a' '${upstreamLib.absolutePath}'
            """
                    .trimIndent(),
            )
        }
}

// Android NDK native TagLib builds per ABI

val androidNdkVersion = System.getenv("ANDROID_NDK_VERSION") ?: "30.0.15729638"
val androidHome =
    System.getenv("ANDROID_HOME")
        ?: "${System.getProperty("user.home")}/Library/Android/sdk"
val androidNdkDir = file("$androidHome/ndk/$androidNdkVersion")
val androidTagLibToolchainFile =
    file("$androidNdkDir/build/cmake/android.toolchain.cmake")

val androidTagLibAbis =
    extensions.getByType<RhythHausAndroidAbiContractExtension>().abis.get()
val androidTagLibMinSdk = libs.versions.android.minSdk.get().toInt()

val androidTagLibOutputRoot =
    layout.buildDirectory.dir("generated/androidNativeLibs").get().asFile
val androidTagLibPackagedJniLibsRoot =
    layout.projectDirectory.dir("src/androidMain/jniLibs").asFile

val androidNativeBuildTasks =
    androidTagLibAbis
        .map { abi ->
            val ndkAbi =
                when (abi) {
                    "arm64-v8a" -> "aarch64-linux-android"
                    "armeabi-v7a" -> "armv7a-linux-androideabi"
                    "x86_64" -> "x86_64-linux-android"
                    else -> error("unsupported ABI: $abi")
                }
            val ndkToolchain = "${ndkAbi}$androidTagLibMinSdk"
            val buildDir =
                layout.buildDirectory
                    .dir("native/androidTagLibHelper-$abi")
                    .get()
                    .asFile
            val outputSo =
                androidTagLibOutputRoot.resolve("$abi/librhythhaus_taglib.so")

            abi to
                tasks.register("buildAndroidTagLibHelper-$abi", Exec::class) {
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
            """
                            .trimIndent(),
                    )
                }
        }
        .toMap()

val buildAllAndroidTagLibHelpers by tasks.registering {
    dependsOn(androidNativeBuildTasks.values)
}

val packageAndroidTagLibJniLibs by
    tasks.registering(Sync::class) {
        dependsOn(buildAllAndroidTagLibHelpers)
        from(androidTagLibOutputRoot)
        into(androidTagLibPackagedJniLibsRoot)
        include("**/librhythhaus_taglib.so")
    }

kotlin {
    jvm()

    android {
        namespace = "com.eterocell.rhythhaus.taglib"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Android native TagLib packaging uses the same native/CMake
        // FetchContent-pinned upstream
        // github.com/taglib/taglib source as JVM/macOS (v2.3 commit
        // 1b94b93762636ebe5733180c3e825be4621e4c7f).
        // Per-ABI .so slices are built by buildAndroidTagLibHelper-<abi> Exec
        // tasks and output to
        // src/androidMain/jniLibs/<abi>/librhythhaus_taglib.so through
        // packageAndroidTagLibJniLibs.
        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    listOf(
            iosArm64(),
            iosSimulatorArm64(),
        )
        .forEach { iosTarget ->
            val targetName = iosTarget.name
            val staticLibrary = iosTagLibStaticLibraries.getValue(targetName)
            val upstreamLibrary =
                iosTagLibUpstreamLibraries.getValue(targetName)
            val nativeBuildTask = iosNativeBuildTasks.getValue(targetName)
            val generatedDefFile =
                layout.buildDirectory.file(
                    "generated/cinteropDefs/$targetName/rh_taglib.def")
            val templateFile =
                project.file("src/nativeInterop/cinterop/rh_taglib.def")
            val generateDefTask =
                tasks.register("generateDef$targetName") {
                    dependsOn(nativeBuildTask)
                    inputs.file(templateFile)
                    outputs.file(generatedDefFile)
                    val targetOutputFile = generatedDefFile.get().asFile
                    val libPath = staticLibrary.absolutePath
                    val libDir = staticLibrary.parentFile.absolutePath
                    val upstreamLibPath = upstreamLibrary.absolutePath
                    doLast {
                        val template = templateFile.readText()
                        val parentDir =
                            targetOutputFile.parentFile.also { it.mkdirs() }
                        val resolved =
                            template
                                .replace(
                                    "# GENERATED_LIBRARY_PATH",
                                    "libraryPaths = $libDir",
                                )
                                .replace(
                                    "# GENERATED_STATIC_LIBRARY",
                                    "staticLibraries = librhythhaus_taglib.a libtag.a",
                                )
                        parentDir.resolve("rh_taglib.def").writeText(resolved)
                    }
                }
            iosTarget.compilations.getByName("main") {
                cinterops.create("rh_taglib") {
                    defFile(generatedDefFile)
                    includeDirs(project.file("native/include"))
                }
            }
            iosTarget.binaries.all {
                linkerOpts("-lc++")
            }
            iosTarget.compilations.configureEach {
                compileTaskProvider.configure {
                    dependsOn(generateDefTask, nativeBuildTask)
                }
            }
            tasks.configureEach {
                if (name ==
                    "cinteropRh_taglib${targetName.replaceFirstChar { it.uppercase() }}") {
                    dependsOn(generateDefTask)
                }
            }
        }

    sourceSets {
        jvmMain {
            resources.srcDir(nativeTagLibResourceRoot)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks
    .matching {
        it.name in setOf("jvmProcessResources", "processJvmMainResources")
    }
    .configureEach {
        dependsOn(buildMacosTagLibHelper)
    }

// Wire Android native TagLib builds before AGP merges jniLibs.
// AGP automatically packages jniLibs from
// build/generated/androidNativeLibs/<abi>/.
tasks
    .matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }
    .configureEach {
        dependsOn(packageAndroidTagLibJniLibs)
    }

tasks
    .matching {
        it.name.contains("copy") && it.name.contains("JniLibsProjectOnly")
    }
    .configureEach {
        dependsOn(packageAndroidTagLibJniLibs)
    }
