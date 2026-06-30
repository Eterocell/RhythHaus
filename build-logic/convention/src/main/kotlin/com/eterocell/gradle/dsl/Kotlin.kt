package com.eterocell.gradle.dsl

import org.gradle.api.Plugin
import org.gradle.api.Project

fun Project.withKotlin(block: Plugin<in Any>.() -> Unit) {
    plugins.withId("kotlin", block)
    withKotlinMultiplatform(block)
}

fun Project.withKotlinMultiplatform(block: Plugin<in Any>.() -> Unit) =
    plugins.withId("kotlin-multiplatform", block)

fun Project.withKotlinDsl(block: Plugin<in Any>.() -> Unit) =
    plugins.withId("org.gradle.kotlin.kotlin-dsl", block)
