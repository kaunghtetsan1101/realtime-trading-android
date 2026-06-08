plugins {
    alias(libs.plugins.tradingapp.android.library.compose)
}

android {
    namespace = "com.tradingapp.designsystem"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
}
