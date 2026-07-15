plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("androidAbiContract") {
            id = "build-logic.android.abi-contract"
            implementationClass = "com.eterocell.gradle.android.AndroidAbiContractPlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.plugin.android)
    implementation(libs.gradle.plugin.ksp)
    implementation(libs.gradle.plugin.spotless)
    implementation(libs.gradle.plugin.kotlin)
    implementation(libs.gradle.plugin.compose.compiler)

    testImplementation(gradleTestKit())
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit)
}
