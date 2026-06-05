pluginManagement {
    includeBuild("build-logic")
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
include(":core-navigation")
include(":domain")
include(":data")
include(":feature-watchlist")
include(":feature-market-detail")
include(":feature-search")
include(":feature-trading")
include(":baseline-profile")
include(":macrobenchmark")

include(":core-designsystem")
include(":core-datastore")
include(":feature-settings")
