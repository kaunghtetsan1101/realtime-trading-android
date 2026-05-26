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

rootProject.name = "RealtimeTrading"

include(":app")
include(":core-common")
include(":core-ui")
include(":core-network")
include(":core-database")
include(":domain")
include(":data")
include(":feature-watchlist")

// Future modules — uncomment when ready:
// include(":core-designsystem")
// include(":feature-market-detail")
// include(":feature-search")
// include(":feature-settings")
