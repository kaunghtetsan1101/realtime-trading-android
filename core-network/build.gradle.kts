plugins {
    id("tradingapp.android.library")
    id("tradingapp.android.hilt")
}

android {
    namespace = "com.tradingapp.network"
}

dependencies {
    implementation(project(":core-common"))
    implementation(libs.findLibrary("retrofit").get())
    implementation(libs.findLibrary("retrofit-gson").get())
    implementation(libs.findLibrary("okhttp").get())
    implementation(libs.findLibrary("okhttp-logging").get())
    implementation(libs.findLibrary("coroutines-core").get())
    implementation(libs.findLibrary("timber").get())

    testImplementation(libs.findLibrary("junit4").get())
    testImplementation(libs.findLibrary("mockk").get())
    testImplementation(libs.findLibrary("coroutines-test").get())
    testImplementation(libs.findLibrary("turbine").get())
    testImplementation(libs.findLibrary("okhttp-mockwebserver").get())
}
