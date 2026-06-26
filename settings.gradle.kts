pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
    }
    versionCatalogs {
        create("libs") {
            from("com.what3words:android-version-catalog:2026.06.01-SNAPSHOT")

            // ---- Local overrides ----
            // Pin minSdk for this library; the shared catalog default differs.
            version("minSdk", "24")
        }
    }
}
rootProject.name = "ocr-components-sample"
include(":lib")
