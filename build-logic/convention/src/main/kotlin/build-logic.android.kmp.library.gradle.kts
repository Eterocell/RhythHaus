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

androidComponents.finalizeDsl {
    android: KotlinMultiplatformAndroidLibraryExtension ->
    resourcesEnabled = android.androidResources.enable
    namespace = android.namespace?.takeIf(String::isNotBlank) ?: "<invalid>"
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
