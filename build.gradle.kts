buildscript {
    dependencies {
        classpath(libs.ktlint.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.jacoco.core)
        // Provides javax.activation.* for AGP's JAXB; see settings.gradle.kts for details.
        classpath(libs.javax.activation)
    }
}

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.gradle.ktlint) apply false
    alias(libs.plugins.jreleaser) apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}
