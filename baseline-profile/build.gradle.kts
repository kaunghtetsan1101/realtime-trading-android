plugins {
    id("com.android.test")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.tradingapp.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // Baseline profiles require API 28+
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.androidx.junit.v130)
    implementation(libs.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4.v150alpha06)
}
