plugins {
    id("tradingapp.android.library")
    id("tradingapp.android.hilt")
}

android {
    namespace = "com.tradingapp.datastore"
}

dependencies {
    implementation(libs.findLibrary("androidx-datastore-preferences").get())
    implementation(libs.findLibrary("coroutines-core").get())
}
