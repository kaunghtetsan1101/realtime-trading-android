plugins {
    id("tradingapp.android.library.compose")
}

android {
    namespace = "com.tradingapp.ui"
}

dependencies {
    implementation(platform(libs.findLibrary("compose-bom").get()))
    implementation(libs.findLibrary("compose-ui").get())
    implementation(libs.findLibrary("compose-ui-graphics").get())
    implementation(libs.findLibrary("compose-ui-tooling-preview").get())
    implementation(libs.findLibrary("compose-material3").get())
    implementation(libs.findLibrary("compose-material-icons-extended").get())
    debugImplementation(libs.findLibrary("compose-ui-tooling").get())
}
