plugins {
    id("tradingapp.android.library")
    id("tradingapp.android.hilt")
}

android {
    namespace = "com.tradingapp.database"
}

dependencies {
    implementation(project(":core-common"))
    implementation(libs.findLibrary("room-runtime").get())
    implementation(libs.findLibrary("room-ktx").get())
    ksp(libs.findLibrary("room-compiler").get())
    implementation(libs.findLibrary("coroutines-core").get())

    testImplementation(libs.findLibrary("junit4").get())
    testImplementation(libs.findLibrary("coroutines-test").get())
    testImplementation(libs.findLibrary("turbine").get())
}
