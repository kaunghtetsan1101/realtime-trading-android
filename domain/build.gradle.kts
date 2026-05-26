plugins {
    alias(libs.plugins.kotlin.jvm)
}



dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-common"))
    implementation(project(":core-common"))
    implementation(libs.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}
