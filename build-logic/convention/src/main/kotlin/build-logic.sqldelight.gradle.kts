import app.cash.sqldelight.gradle.SqlDelightExtension
import com.eterocell.gradle.architecture.ArchitectureModelRegistry
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("app.cash.sqldelight")
}

afterEvaluate {
    val extension = extensions.getByType<SqlDelightExtension>()
    val sourceSets =
        extensions
            .findByType<KotlinMultiplatformExtension>()
            ?.sourceSets
            ?.filter { it.name.endsWith("Main") }
            ?.map { it.name }
            .orEmpty()
    val roots =
        extension.databases.flatMap { database ->
            val defaultRoots = sourceSets.map { sourceSet ->
                file("src/$sourceSet/sqldelight")
            }
            (defaultRoots + database.srcDirs.files).distinct().map { root ->
                val valid = sourceSets.any { sourceSet ->
                    root
                        .toPath()
                        .normalize()
                        .startsWith(file("src/$sourceSet").toPath().normalize())
                }
                ArchitectureModelRegistry.SqlDelightRootRecord(
                    module = path,
                    database = database.name,
                    root = root,
                    status = if (valid) "VALID" else "UNSUPPORTED",
                )
            }
        }
    val productionConfigurations =
        setOf(
                "commonMainImplementation",
                "jvmMainImplementation",
                "androidMainImplementation",
                "iosMainImplementation",
                "iosArm64MainImplementation",
                "iosSimulatorArm64MainImplementation",
                "iosX64MainImplementation",
            )
            .mapNotNull(configurations::findByName)
    val hasDriver = productionConfigurations.any { configuration ->
        configuration.dependencies.any { dependency ->
            dependency.group == "app.cash.sqldelight" &&
                dependency.name in
                    setOf("android-driver", "native-driver", "sqlite-driver")
        }
    }
    ArchitectureModelRegistry.forRoot(this)
        .publishSqlDelight(
            owner = path,
            roots = roots,
            isOwner = extension.databases.isNotEmpty() && hasDriver,
        )
}

sqldelight {
    databases {
        create("RhythHausDatabase") {
            packageName.set("com.eterocell.rhythhaus.library")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.3.2")
            schemaOutputDirectory.set(
                file("src/commonMain/sqldelight/databases"))
        }
    }
}
