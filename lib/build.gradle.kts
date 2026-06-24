import java.util.Base64
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.dokka)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jreleaser)
    id("kotlin-parcelize")
    id("jacoco")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
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

kotlin {
    compilerOptions {
        // Aligns with the shared catalog jvmToolchain (17).
        jvmTarget = JvmTarget.JVM_17
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

//region publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            afterEvaluate {
                from(components["release"])
            }

            groupId = "com.what3words"
            artifactId = "w3w-android-ocr-components"
            version = project.version.toString()

            withType(MavenPublication::class.java) {
                val publicationName = name
                val dokkaJar =
                    project.tasks.register("${publicationName}DokkaJar", Jar::class) {
                        group = JavaBasePlugin.DOCUMENTATION_GROUP
                        description = "Assembles Kotlin docs with Dokka into a Javadoc jar"
                        archiveClassifier.set("javadoc")
                        from(tasks.named("dokkaGeneratePublicationHtml"))

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
            // POM metadata
        }
    }

    repositories {
        maven {
            name = "sonatypeSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            credentials {
                username = findProperty("MAVEN_CENTRAL_USERNAME") as? String
                password = findProperty("MAVEN_CENTRAL_PASSWORD") as? String
            }
        }
        maven {
            name = "stagingLocal"
            url = uri(layout.buildDirectory.dir("staging-deploy").get().asFile.absolutePath)
        }
    }
}

jreleaser {
    release {
        github {
            repoOwner = "what3words"
            overwrite = true
        }
    }

    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
        publicKey.set(
            findProperty("W3W_GPG_PUBLIC_KEY")?.toString()
                ?.let { String(Base64.getDecoder().decode(it)) } ?: "")
        secretKey.set(
            findProperty("W3W_GPG_SECRET_KEY")?.toString()
                ?.let { String(Base64.getDecoder().decode(it)) } ?: "")
        passphrase.set(findProperty("W3W_GPG_PASSPHRASE")?.toString())
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().asFile.absolutePath)
                    username.set(findProperty("MAVEN_CENTRAL_USERNAME")?.toString())
                    password.set(findProperty("MAVEN_CENTRAL_PASSWORD")?.toString())
                    verifyPom.set(false)
                    setStage(org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer.Stage.UPLOAD.toString())
                }
            }
        }
    }
}
//endregion

//endregion
