plugins {
    id("tradingapp.android.library.compose")
    id("tradingapp.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tradingapp.navigation"
}

dependencies {
    implementation(project(":feature-watchlist"))
    implementation(project(":feature-market-detail"))
    implementation(project(":feature-search"))
    implementation(project(":feature-trading"))
    implementation(project(":feature-settings"))

    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.nav3)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.serialization.core)

    debugImplementation(libs.compose.ui.tooling)
}
