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
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
    }
    versionCatalogs {
        create("libs") {
            from("com.what3words:android-version-catalog:2026.06.01-SNAPSHOT")

            // ---- Local overrides ----
            // Pin minSdk for this library; the shared catalog default differs.
            version("minSdk", "24")

            // ---- Local additions (libraries not present in the shared catalog) ----
            // Legacy JavaBeans Activation Framework (javax.activation namespace).
            // AGP's bundled JAXB (glassfish jaxb-runtime 2.3.2) needs javax.activation.*,
            // but JReleaser drags jakarta.activation-api up to 2.x (jakarta namespace) on the
            // shared build classpath, so we add the legacy JAF jar to keep both namespaces available.
            library("javax-activation", "javax.activation", "activation").version("1.1.1")
        }
    }
}
rootProject.name = "ocr-components-sample"
include(":lib")
