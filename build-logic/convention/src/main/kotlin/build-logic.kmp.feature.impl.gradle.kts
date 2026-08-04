import com.eterocell.gradle.architecture.ArchitectureAllowList
import com.eterocell.gradle.architecture.ArchitectureModelRegistry
import com.google.devtools.ksp.gradle.KspExtension
import java.io.File
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

if (rootProject.findProject(":architecture-processor") != null) {
    pluginManager.apply("com.google.devtools.ksp")

    extensions.configure<KspExtension> {
        arg("architecture.module", project.path)
        arg(
            "architecture.packageRoots",
            ArchitectureAllowList.packageRoots(project.path)
                .sorted()
                .joinToString(","),
        )
    }

    afterEvaluate {
        val sourceRoots =
            extensions
                .getByType<
                    org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
                .sourceSets
                .filterNot { it.name.contains("test", ignoreCase = true) }
                .flatMap { it.kotlin.srcDirs }
                .map(File::getAbsolutePath)
                .sorted()
        extensions.configure<KspExtension> {
            arg(
                "architecture.sourceRoots",
                sourceRoots.joinToString(File.pathSeparator))
        }
    }

    extensions
        .getByType<
            org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
        .targets
        .configureEach {
            if (name != "metadata") {
                val targetName = name
                compilations.configureEach {
                    if (name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                        val configurationName =
                            "ksp${targetName.replaceFirstChar(Char::uppercaseChar)}"
                        configurations.named(configurationName) {
                            dependencies.add(
                                project.dependencies.project(
                                    mapOf("path" to ":architecture-processor")))
                        }
                        ArchitectureModelRegistry.forRoot(project)
                            .publishKspRegistration(
                                ArchitectureModelRegistry.KspRegistration(
                                    project.path,
                                    configurationName,
                                    ":architecture-processor"),
                            )
                    }
                }
            }
        }
}
