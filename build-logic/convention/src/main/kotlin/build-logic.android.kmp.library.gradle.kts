import com.android.build.api.dsl.KotlinMultiplatformAndroidDeviceTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.eterocell.gradle.architecture.ArchitectureModelRegistry
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("com.android.kotlin.multiplatform.library")
}

val androidComponents =
    extensions.getByType<KotlinMultiplatformAndroidComponentsExtension>()
var resourcesEnabled = false
var namespace = "<invalid>"
val productionResourceRoots =
    mutableListOf<Provider<List<Collection<Directory>>>>()

extensions.configure<KotlinMultiplatformExtension> {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

androidComponents.finalizeDsl {
    android: KotlinMultiplatformAndroidLibraryExtension ->
    resourcesEnabled = android.androidResources.enable
    namespace = android.namespace?.takeIf(String::isNotBlank) ?: "<invalid>"
    if (!resourcesEnabled &&
        file("src/commonMain/composeResources").isDirectory) {
        error(
            "$path has Compose resources under src/commonMain/composeResources but " +
                "androidResources.enable is false; add `androidResources { enable = true }` " +
                "to the android {} block so Android packages them.",
        )
    }
}

androidComponents.onVariants { variant ->
    if (!resourcesEnabled) return@onVariants

    variant.sources.res?.static?.let { roots ->
        productionResourceRoots.add(roots)
    }
}

afterEvaluate {
    val roots =
        productionResourceRoots.fold(
            providers.provider { emptyList<Directory>() }) { accumulated, next
                ->
                accumulated.zip(next) { previous, current ->
                    (previous + current.flatten()).distinct()
                }
            }
    ArchitectureModelRegistry.forRoot(project)
        .publishResources(
            roots.map { directories ->
                directories
                    .map { root ->
                        ArchitectureModelRegistry.ResourceRecord(
                            module = path,
                            sourceSet = "main",
                            kind = "ANDROID",
                            root = root.asFile,
                            namespace = namespace,
                        )
                    }
                    .sorted()
            },
        )

    val target =
        extensions
            .getByType<KotlinMultiplatformExtension>()
            .targets
            .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
            .single()
    val testConfigurationNames =
        (target.compilations.withType(
                KotlinMultiplatformAndroidHostTestCompilation::class.java) +
                target.compilations.withType(
                    KotlinMultiplatformAndroidDeviceTestCompilation::class
                        .java))
            .flatMap { compilation ->
                listOfNotNull(
                    compilation.compileDependencyConfigurationName,
                    compilation.runtimeDependencyConfigurationName,
                )
            }
            .distinct()
            .sorted()
    testConfigurationNames.map(configurations::getByName).forEach {
        configuration ->
        ArchitectureModelRegistry.forRoot(project)
            .publishAndroidTestConfiguration(project, configuration)
    }
}
