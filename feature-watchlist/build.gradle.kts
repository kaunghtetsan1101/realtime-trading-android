plugins {
    alias(libs.plugins.tradingapp.android.feature)
}

android {
    namespace = "com.tradingapp.watchlist"
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-ui"))
    implementation(project(":domain"))
}
