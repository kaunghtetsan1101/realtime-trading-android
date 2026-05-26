pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
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

include(":feature-market-detail")

// Future modules — uncomment when ready:
// include(":core-designsystem")
// include(":feature-search")
// include(":feature-settings")
