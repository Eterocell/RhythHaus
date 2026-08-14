plugins {
    `kotlin-dsl`
}

tasks.withType<Test>().configureEach {
    systemProperty(
        "rhythhaus.rootDir", projectDir.parentFile.parentFile.absolutePath)
    providers.gradleProperty("rhythhaus.aabProbeFile").orNull?.let { probeFile
        ->
        systemProperty("rhythhaus.aabProbeFile", probeFile)
    }
    providers
        .gradleProperty("rhythhaus.architectureProcessorJar")
        .orElse(
            providers.provider {
                System.getProperty("rhythhaus.architectureProcessorJar")
            })
        .orNull
        ?.let { processorJar ->
            systemProperty("rhythhaus.architectureProcessorJar", processorJar)
        }
}

gradlePlugin {
    plugins {
        create("androidAbiContract") {
            id = "build-logic.android.abi-contract"
            implementationClass =
                "com.eterocell.gradle.android.AndroidAbiContractPlugin"
        }
        create("architectureCheck") {
            id = "build-logic.architecture-check"
            implementationClass =
                "com.eterocell.gradle.architecture.ArchitectureCheckPlugin"
        }
        create("featureScaffold") {
            id = "build-logic.feature-scaffold"
            implementationClass =
                "com.eterocell.gradle.scaffold.FeatureScaffoldPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.plugin.android)
    implementation(libs.gradle.plugin.detekt)
    implementation(libs.gradle.plugin.ksp)
    implementation(libs.gradle.plugin.spotless)
    implementation(libs.gradle.plugin.kotlin)
    implementation(libs.gradle.plugin.compose.compiler)
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.11.1")
    implementation("app.cash.sqldelight:gradle-plugin:2.3.2")

    testImplementation(gradleTestKit())
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit)
}
