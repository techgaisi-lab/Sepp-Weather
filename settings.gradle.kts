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
    }
}

rootProject.name = "Sepp-Weather"

// Die Bibliothek, die in andere Apps eingebunden wird
include(":sepp-weather")

// Kleine Demo-App zum Ausprobieren aller Varianten
include(":sample")
