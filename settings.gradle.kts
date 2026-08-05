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

rootProject.name = "Vantafyn"

include(":app-tv")
include(":app-mobile")
include(":core-jellyfin")
include(":core-media")
include(":core-ui")
include(":feature-home")
include(":feature-library")
include(":feature-player")
