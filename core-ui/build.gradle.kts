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

    implementation(platform(libs.findLibrary("compose-bom").get()))
    implementation(libs.findLibrary("compose-ui").get())
    implementation(libs.findLibrary("compose-ui-graphics").get())
    implementation(libs.findLibrary("compose-ui-tooling-preview").get())
    implementation(libs.findLibrary("compose-material3").get())
    implementation(libs.findLibrary("compose-material-icons-extended").get())
    debugImplementation(libs.findLibrary("compose-ui-tooling").get())
}
