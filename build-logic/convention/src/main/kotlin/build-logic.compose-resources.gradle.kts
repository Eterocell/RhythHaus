import com.eterocell.gradle.architecture.ArchitectureAllowList
import com.eterocell.gradle.architecture.ArchitectureModelRegistry
import com.eterocell.gradle.architecture.ControlledComposeResourcesExtension
import java.io.File
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.compose")
}

var configuredNamespace: String? = null

fun declareNamespace(namespace: String) {
    configuredNamespace = namespace.trim()
    compose.resources {
        packageOfResClass = namespace
    }
}

val architectureComposeResources =
    extensions.create<ControlledComposeResourcesExtension>(
        "architectureComposeResources",
        ::declareNamespace,
    )

providers.gradleProperty("architecture.compose.namespace").orNull?.let {
    architectureComposeResources.namespace(it)
}

extensions
    .findByType<KotlinMultiplatformExtension>()
    ?.sourceSets
    ?.filter { it.name.endsWith("Main") }
    ?.forEach { sourceSet ->
        (ArchitectureAllowList.composeResourceRoots(path, sourceSet.name) +
                providers
                    .gradleProperty(
                        "architecture.compose.${sourceSet.name}.roots")
                    .orNull
                    .orEmpty()
                    .split(File.pathSeparator)
                    .filter(String::isNotBlank))
            .map(::file)
            .filter { root ->
                root
                    .toPath()
                    .normalize()
                    .startsWith(projectDir.toPath().normalize())
            }
            .forEach { root ->
                compose.resources {
                    customDirectory(
                        sourceSet.name,
                        providers.provider {
                            layout.projectDirectory.dir(
                                projectDir
                                    .toPath()
                                    .normalize()
                                    .relativize(root.toPath().normalize())
                                    .toString())
                        },
                    )
                }
            }
    }

afterEvaluate {
    val kotlin =
        extensions.findByType<KotlinMultiplatformExtension>()
            ?: return@afterEvaluate
    val namespace =
        configuredNamespace?.takeIf(String::isNotBlank) ?: "<invalid>"
    val records =
        kotlin.sourceSets
            .filter { it.name.endsWith("Main") }
            .flatMap { sourceSet ->
                val standardRoot =
                    file("src/${sourceSet.name}/composeResources")
                val customRoots =
                    (ArchitectureAllowList.composeResourceRoots(
                            path, sourceSet.name) +
                            providers
                                .gradleProperty(
                                    "architecture.compose.${sourceSet.name}.roots")
                                .orNull
                                .orEmpty()
                                .split(File.pathSeparator)
                                .filter(String::isNotBlank))
                        .map(::file)
                (listOf(standardRoot) + customRoots).distinct().map { root ->
                    val status =
                        if (root
                            .toPath()
                            .normalize()
                            .startsWith(projectDir.toPath().normalize()))
                            "COMPOSE"
                        else "UNSUPPORTED"
                    ArchitectureModelRegistry.ResourceRecord(
                        path, sourceSet.name, status, root, namespace)
                }
            }
    ArchitectureModelRegistry.forRoot(this).publishResources(records)
}
