import com.eterocell.gradle.dsl.libs
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension

plugins {
    id("build-logic.root-project.base")
    id("dev.detekt")
}

val detektVersion = libs.findVersion("detekt").get().requiredVersion

allprojects {
    pluginManager.apply("dev.detekt")

    extensions.configure<DetektExtension> {
        toolVersion = detektVersion
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
    }

    tasks.withType<Detekt>().configureEach {
        reports {
            checkstyle.required.set(false)
            html.required.set(true)
            sarif.required.set(false)
            markdown.required.set(false)
        }
    }
}
