plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tradingapp.navigation"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
    }

    buildFeatures { compose = true }
}

kotlin { jvmToolchain(17) }

dependencies {
    // Feature screens wired by this nav graph
    implementation(project(":feature-watchlist"))
    implementation(project(":feature-market-detail"))
    implementation(project(":feature-search"))

    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose runtime (needed for @Composable + mutableStateListOf)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    // Navigation 3
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.nav3)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Route serialization
    implementation(libs.kotlinx.serialization.core)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    debugImplementation(libs.compose.ui.tooling)
}
