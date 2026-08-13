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
include(":core-integrations")
include(":core-ombi")
include(":core-media")
include(":core-cast")
include(":core-ui")
include(":feature-home")
include(":feature-library")
include(":feature-music")
include(":feature-player")
include(":feature-requests")
