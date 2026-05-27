plugins {
    id("tradingapp.android.library")
    id("tradingapp.android.hilt")
}

android {
    namespace = "com.tradingapp.data"
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-network"))
    implementation(project(":core-database"))
    implementation(project(":domain"))
    implementation(libs.findLibrary("coroutines-core").get())
    implementation(libs.findLibrary("timber").get())

    testImplementation(libs.findLibrary("junit4").get())
    testImplementation(libs.findLibrary("coroutines-test").get())
    testImplementation(libs.findLibrary("turbine").get())
    testImplementation(libs.findLibrary("mockk").get())
}
