import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish)
    id("kotlin-parcelize")
    id("jacoco")
}

group = "com.what3words"

/**
 * Pass `-Psnapshot` on the Gradle command line to publish a `-SNAPSHOT` build.
 * The CI pipeline does this for pushes to the `staging` branch; pushes to `main`
 * publish a regular release.
 */
val isSnapshotRelease = hasProperty("snapshot")
version =
    if (isSnapshotRelease) "${findProperty("LIBRARY_VERSION")}-SNAPSHOT" else "${findProperty("LIBRARY_VERSION")}"

android {
    namespace = "com.what3words.ocr.components"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        named("debug") {
            enableUnitTestCoverage = true
        }
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmToolchain.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmToolchain.get())
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "META-INF/LICENSE*"
        }
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api33") {
                    device = "Pixel 6"
                    apiLevel = 33
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.jvmToolchain.get())
    }
}

dependencies {
    api(libs.androidx.camera.view)
    api(libs.androidx.camera.camera2)
    api(libs.androidx.camera.lifecycle)

    implementation(libs.accompanist.permissions)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    compileOnly(libs.mlkit.textRecognition)
    compileOnly(libs.mlkit.textRecognition.chinese)
    compileOnly(libs.mlkit.textRecognition.devanagari)
    compileOnly(libs.mlkit.textRecognition.japanese)
    compileOnly(libs.mlkit.textRecognition.korean)

    api(libs.w3w.android.wrapper)
    api(libs.w3w.android.design.library)
    api(libs.w3w.core.multiplatform)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.coil.kt.coil.compose)

    implementation(libs.gms.base)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.junit.jupiter.api)
    androidTestImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)

    androidTestImplementation(libs.mlkit.textRecognition.bundled)
    androidTestImplementation(libs.mlkit.textRecognition.bundled.chinese)
    androidTestImplementation(libs.mlkit.textRecognition.bundled.devanagari)
    androidTestImplementation(libs.mlkit.textRecognition.bundled.japanese)
    androidTestImplementation(libs.mlkit.textRecognition.bundled.korean)
}

tasks.register("checkSnapshotDependencies") {
    doLast {
        val snapshotDependencies = allprojects.flatMap { project ->
            project.configurations
                .asSequence()
                .filter { it.isCanBeResolved }
                .flatMap { it.allDependencies }
                .filter { it.version?.contains("SNAPSHOT", ignoreCase = true) == true }
                .map { "${project.name}:${it.group}:${it.name}:${it.version}" }
                .distinct()
                .toList()
        }

        if (snapshotDependencies.isNotEmpty()) {
            snapshotDependencies.forEach { println("SNAPSHOT dependency found: $it") }
            throw GradleException("SNAPSHOT dependencies found.")
        } else {
            println("No SNAPSHOT dependencies found.")
        }
    }
}

//region publishing
mavenPublishing {
    coordinates("com.what3words", "w3w-android-ocr-components", version.toString())

    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("w3w-android-ocr-components")
        description.set("Android OCR UI Components that work with Google MLKit")
        url.set("https://github.com/what3words/w3w-android-ocr-components")

        licenses {
            license {
                name.set("The MIT License (MIT)")
                url.set("https://github.com/what3words/w3w-android-ocr-components/blob/master/LICENSE")
            }
        }
        developers {
            developer {
                id.set("what3words")
                name.set("what3words")
                email.set("development@what3words.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/what3words/w3w-android-ocr-components.git")
            developerConnection.set("scm:git:ssh://git@github.com:what3words/w3w-android-ocr-components.git")
            url.set("https://github.com/what3words/w3w-android-ocr-components/tree/master")
        }
    }
}
//endregion
