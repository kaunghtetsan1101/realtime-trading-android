plugins {
    alias(libs.plugins.tradingapp.kotlin.library)
}

dependencies {
    implementation(project(":core-common"))
    implementation(libs.javax.inject)
}
