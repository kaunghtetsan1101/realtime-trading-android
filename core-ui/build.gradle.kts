plugins {
    id("tradingapp.android.library.compose")
}

android {
    namespace = "com.tradingapp.ui"
}

dependencies {
    // api — design tokens (Color, Spacing, Shape, Typography) are part of core-ui's public surface
    // so consumers of core-ui can use them without a separate explicit dep.
    api(project(":core-designsystem"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    debugImplementation(libs.compose.ui.tooling)
}
