import java.net.URI

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("maven-publish")
    id("jacoco")
    id("signing")
    id("org.jetbrains.dokka") version "1.5.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
}

group = "com.what3words"

/**
 * IS_SNAPSHOT_RELEASE property will be automatically added to the root gradle.properties file by the CI pipeline, depending on the GitHub branch.
 * A snapshot release is generated for every pull request merged or commit made into an epic branch.
 */
val isSnapshotRelease = findProperty("IS_SNAPSHOT_RELEASE") == "true"
version =
    if (isSnapshotRelease) "${findProperty("LIBRARY_VERSION")}-SNAPSHOT" else "${findProperty("LIBRARY_VERSION")}"

android {
    namespace = "com.what3words.ocr.components"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.6"
    }
    packaging {
        resources {
            excludes += "META-INF/LICENSE*"
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
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

dependencies {
    // CameraX
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // MLKit
    compileOnly("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    compileOnly("com.google.android.gms:play-services-mlkit-text-recognition-chinese:16.0.1")
    compileOnly("com.google.android.gms:play-services-mlkit-text-recognition-devanagari:16.0.1")
    compileOnly("com.google.android.gms:play-services-mlkit-text-recognition-japanese:16.0.1")
    compileOnly("com.google.android.gms:play-services-mlkit-text-recognition-korean:16.0.1")

    // what3words
    api("com.what3words:w3w-android-wrapper:4.0.2")
    api("com.what3words:w3w-android-design-library:2.0.2")
    api("com.what3words:w3w-core-android:1.1.0-SNAPSHOT") {
        isChanging = true
    }

    //compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.constraintlayout:constraintlayout-compose")
    implementation("androidx.compose.material3:material3")

    // gms for module install
    implementation("com.google.android.gms:play-services-base:18.5.0")

    // Test dependencies
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestUtil("androidx.test:orchestrator:1.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.3")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    androidTestImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")

    androidTestImplementation("com.google.mlkit:text-recognition:16.0.1")
    androidTestImplementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    androidTestImplementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
    androidTestImplementation("com.google.mlkit:text-recognition-japanese:16.0.1")
    androidTestImplementation("com.google.mlkit:text-recognition-korean:16.0.1")
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
            throw GradleException("SNAPSHOT dependencies found. Remove them before merging to staging.")
        } else {
            println("No SNAPSHOT dependencies found.")
        }
    }
}

//region publishing

val ossrhUsername = findProperty("OSSRH_USERNAME") as String?
val ossrhPassword = findProperty("OSSRH_PASSWORD") as String?
val signingKey = findProperty("SIGNING_KEY") as String?
val signingKeyPwd = findProperty("SIGNING_KEY_PWD") as String?

publishing {
    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl =
                "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl =
                "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = if (version.toString()
                    .endsWith("SNAPSHOT")
            ) URI.create(snapshotsRepoUrl) else URI.create(releasesRepoUrl)

            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
        publications {
            create<MavenPublication>("Maven") {
                artifactId = "w3w-android-ocr-components"
                groupId = "com.what3words"
                version = project.version.toString()
                afterEvaluate {
                    from(components["release"])
                }
            }
            withType(MavenPublication::class.java) {
                val publicationName = name
                val dokkaJar =
                    project.tasks.register("${publicationName}DokkaJar", Jar::class) {
                        group = JavaBasePlugin.DOCUMENTATION_GROUP
                        description = "Assembles Kotlin docs with Dokka into a Javadoc jar"
                        archiveClassifier.set("javadoc")
                        from(tasks.named("dokkaHtml"))

                        // Each archive name should be distinct, to avoid implicit dependency issues.
                        // We use the same format as the sources Jar tasks.
                        // https://youtrack.jetbrains.com/issue/KT-46466
                        archiveBaseName.set("${archiveBaseName.get()}-$publicationName")
                    }
                artifact(dokkaJar)
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
        }
    }
}

signing {
    useInMemoryPgpKeys(signingKey, signingKeyPwd)
    sign(publishing.publications)
}

//endregion