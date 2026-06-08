plugins {
    id("tradingapp.android.library")
    id("tradingapp.android.hilt")
}

android {
    namespace = "com.tradingapp.datastore"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coroutines.core)
}
