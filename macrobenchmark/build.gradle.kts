plugins {
    id("com.android.test")
}

android {
    namespace = "com.tradingapp.macrobenchmark"
    compileSdk = 37

    defaultConfig {
        // Macrobenchmarks require API 29+
        minSdk = 29
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.junit.v130)
    implementation(libs.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4.v150alpha06)
}

androidComponents {
    beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" }
}
