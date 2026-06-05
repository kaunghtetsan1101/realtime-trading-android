plugins {
    alias(libs.plugins.tradingapp.kotlin.library)
}

dependencies {
    testImplementation(libs.findLibrary("junit4").get())
}
