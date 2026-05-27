plugins {
    `kotlin-dsl`
}

group = "com.tradingapp.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("kotlinLibrary") {
            id = "tradingapp.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "tradingapp.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "tradingapp.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "tradingapp.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "tradingapp.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "tradingapp.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
