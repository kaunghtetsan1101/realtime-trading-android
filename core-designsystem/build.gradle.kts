plugins {
    alias(libs.plugins.tradingapp.android.library.compose)
}

android {
    namespace = "com.tradingapp.designsystem"
}

dependencies {
    implementation(platform(libs.findLibrary("compose-bom").get()))
    implementation(libs.findLibrary("compose-ui-graphics").get())
    implementation(libs.findLibrary("compose-material3").get())
}
