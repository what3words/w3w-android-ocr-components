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
        google()
        mavenCentral()
        maven(url="https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}
rootProject.name = "ocr-components-sample"
include(":lib")
include(":what3words")