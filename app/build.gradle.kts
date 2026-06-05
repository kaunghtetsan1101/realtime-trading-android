plugins {
    id("tradingapp.android.application")
    id("tradingapp.android.hilt")
}

android {
    namespace = "com.tradingapp"

    defaultConfig {
        applicationId = "com.tradingapp"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    // Navigation graph — pulls in all feature modules transitively
    implementation(project(":core-navigation"))

    // Infrastructure modules (Hilt component aggregation)
    implementation(project(":core-common"))
    implementation(project(":core-ui"))
    implementation(project(":core-network"))
    implementation(project(":core-database"))
    implementation(project(":core-datastore"))
    implementation(project(":domain"))
    implementation(project(":data"))
}
