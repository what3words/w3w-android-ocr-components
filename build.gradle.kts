buildscript {
    dependencies {
        classpath(libs.ktlint.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.jacoco.core)
    }
}

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.gradle.ktlint) apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}
