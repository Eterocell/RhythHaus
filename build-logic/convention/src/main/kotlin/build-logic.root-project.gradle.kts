import com.eterocell.gradle.architecture.ArchitectureCheckPlugin
import java.net.URI

plugins {
    id("build-logic.root-project.base")
    id("build-logic.spotless")
    id("build-logic.detekt")
}

apply<ArchitectureCheckPlugin>()

val architectureCheck = tasks.named("architectureCheck")
val qualityCheck =
    tasks.register("qualityCheck") {
        group = "verification"
        description = "Runs the repository quality checks."
        dependsOn(architectureCheck)
    }

tasks.named("check") {
    dependsOn(architectureCheck)
}

allprojects {
    qualityCheck.configure {
        dependsOn(tasks.named("detekt"))
        dependsOn(tasks.named("spotlessCheck"))
    }
}

val wrapper: Wrapper by
    tasks.named<Wrapper>("wrapper") {
        gradleVersion = "9.6.1"
        distributionType = Wrapper.DistributionType.ALL
        val sha256 =
            URI.create("$distributionUrl.sha256").toURL().openStream().use {
                it.reader().readText().trim()
            }
        distributionSha256Sum = sha256
    }
