plugins {
    alias(libs.plugins.tradingapp.android.feature)
}

android {
    namespace = "com.tradingapp.marketdetail"
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-ui"))
    implementation(project(":domain"))
}
