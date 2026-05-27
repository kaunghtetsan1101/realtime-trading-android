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

    implementation(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())

    implementation(platform(libs.findLibrary("compose-bom").get()))
    implementation(libs.findLibrary("compose-ui").get())

    implementation(libs.findLibrary("navigation3-runtime").get())
    implementation(libs.findLibrary("navigation3-ui").get())
    implementation(libs.findLibrary("lifecycle-viewmodel-nav3").get())
    implementation(libs.findLibrary("hilt-lifecycle-viewmodel-compose").get())

    implementation(libs.findLibrary("kotlinx-serialization-core").get())

    debugImplementation(libs.findLibrary("compose-ui-tooling").get())
}
