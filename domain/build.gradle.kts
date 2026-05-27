plugins {
    alias(libs.plugins.tradingapp.kotlin.library)
}

dependencies {
    implementation(project(":core-common"))
    implementation("javax.inject:javax.inject:1")
}
