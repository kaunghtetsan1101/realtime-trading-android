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
                compileSdk = catalog.findVersion("compileSdk").get().requiredVersion.toInt()
                defaultConfig.minSdk = catalog.findVersion("minSdk").get().requiredVersion.toInt()
                defaultConfig.targetSdk = catalog.findVersion("targetSdk").get().requiredVersion.toInt()
                buildFeatures.compose = true
                buildFeatures.buildConfig = true
            }
            extensions.configure<KotlinAndroidProjectExtension> {
                jvmToolchain(21)
            }
            dependencies {
                "implementation"(catalog.findLibrary("androidx-core-ktx").get())
                "implementation"(catalog.findLibrary("androidx-lifecycle-runtime-ktx").get())
                "implementation"(catalog.findLibrary("androidx-activity-compose").get())
                "implementation"(platform(catalog.findLibrary("compose-bom").get()))
                "implementation"(catalog.findLibrary("compose-ui").get())
                "implementation"(catalog.findLibrary("compose-ui-graphics").get())
                "implementation"(catalog.findLibrary("compose-ui-tooling-preview").get())
                "implementation"(catalog.findLibrary("compose-material3").get())
                // Baseline Profile: pre-compiles hot paths at install time
                "implementation"(catalog.findLibrary("profile-installer").get())
                "implementation"(catalog.findLibrary("timber").get())
                "debugImplementation"(catalog.findLibrary("leak-canary").get())
                "debugImplementation"(catalog.findLibrary("compose-ui-tooling").get())
                "debugImplementation"(catalog.findLibrary("compose-ui-test-manifest").get())
                "androidTestImplementation"(platform(catalog.findLibrary("compose-bom").get()))
                "androidTestImplementation"(catalog.findLibrary("compose-ui-test-junit4").get())
                "androidTestImplementation"(catalog.findLibrary("junit-ext").get())
                "androidTestImplementation"(catalog.findLibrary("espresso-core").get())
                "testImplementation"(catalog.findLibrary("junit4").get())
            }
        }
    }
}
