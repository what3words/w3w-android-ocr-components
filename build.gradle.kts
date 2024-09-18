buildscript {
    dependencies {
        classpath("org.jacoco:org.jacoco.core:0.8.9")
        classpath("com.android.tools.build:gradle:8.5.0")
    }
}
plugins {
    id("com.android.library") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.20" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "1.8.20" apply false
    id("com.autonomousapps.dependency-analysis") version "1.20.0"
}