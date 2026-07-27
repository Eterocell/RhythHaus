import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.eterocell.gradle.architecture.ArchitectureModelRegistry
import com.eterocell.gradle.dsl.configureAppSigningConfigsForRelease
import org.gradle.api.Action

plugins {
    id("com.android.application")
}

configureAppSigningConfigsForRelease()

extensions.getByType<ApplicationAndroidComponentsExtension>().finalizeDsl {
    android ->
    val namespace = android.namespace?.takeIf(String::isNotBlank) ?: "<invalid>"
    val roots =
        android.sourceSets.getByName("main").res.directories.map(project::file)
    ArchitectureModelRegistry.forRoot(project)
        .publishResources(
            roots.map { root ->
                ArchitectureModelRegistry.ResourceRecord(
                    module = path,
                    sourceSet = "main",
                    kind = "ANDROID",
                    root = root,
                    namespace = namespace,
                )
            },
        )
}

val androidComponents =
    extensions.getByType<ApplicationAndroidComponentsExtension>()
val publishAndroidTestConfigurations: Action<ApplicationVariant> =
    object : Action<ApplicationVariant> {
        override fun execute(variant: ApplicationVariant) {
            val registry = ArchitectureModelRegistry.forRoot(project)
            (variant.deviceTests.values + variant.hostTests.values)
                .flatMap { component ->
                    listOf(
                        component.compileConfiguration,
                        component.runtimeConfiguration)
                }
                .distinctBy { configuration -> configuration.name }
                .forEach { configuration ->
                    registry.publishAndroidTestConfiguration(
                        project, configuration)
                }
        }
    }

androidComponents.onVariants(
    androidComponents.selector().all(), publishAndroidTestConfigurations)
