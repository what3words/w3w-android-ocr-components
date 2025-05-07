buildscript {
    dependencies {
        classpath(libs.ktlint.gradle)
        classpath(libs.sonarqube.gradle.plugin)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.jacoco.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.gradle.ktlint) apply false
    alias(libs.plugins.autonomousapps.analysis)
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}