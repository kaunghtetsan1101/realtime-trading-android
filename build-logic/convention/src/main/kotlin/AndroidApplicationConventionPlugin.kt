import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // AGP 9.0 includes built-in Kotlin support — do not apply kotlin.android manually
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.configure<ApplicationExtension> {
                compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()
                defaultConfig.minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
                defaultConfig.targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                buildFeatures.compose = true
                buildFeatures.buildConfig = true
            }
            extensions.configure<KotlinAndroidProjectExtension> {
                jvmToolchain(17)
            }
            dependencies {
                "implementation"(libs.findLibrary("androidx-core-ktx").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                "implementation"(libs.findLibrary("androidx-activity-compose").get())
                "implementation"(platform(libs.findLibrary("compose-bom").get()))
                "implementation"(libs.findLibrary("compose-ui").get())
                "implementation"(libs.findLibrary("compose-ui-graphics").get())
                "implementation"(libs.findLibrary("compose-ui-tooling-preview").get())
                "implementation"(libs.findLibrary("compose-material3").get())
                // Baseline Profile: pre-compiles hot paths at install time
                "implementation"(libs.findLibrary("profileinstaller").get())
                "implementation"(libs.findLibrary("timber").get())
                "debugImplementation"(libs.findLibrary("leakcanary").get())
                "debugImplementation"(libs.findLibrary("compose-ui-tooling").get())
                "debugImplementation"(libs.findLibrary("compose-ui-test-manifest").get())
                "androidTestImplementation"(platform(libs.findLibrary("compose-bom").get()))
                "androidTestImplementation"(libs.findLibrary("compose-ui-test-junit4").get())
                "androidTestImplementation"(libs.findLibrary("junit-ext").get())
                "androidTestImplementation"(libs.findLibrary("espresso-core").get())
                "testImplementation"(libs.findLibrary("junit4").get())
            }
        }
    }
}
