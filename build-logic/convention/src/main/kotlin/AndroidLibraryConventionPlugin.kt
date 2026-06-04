import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // AGP 9.0 includes built-in Kotlin support — do not apply kotlin.android manually
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> {
                compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()
                defaultConfig.minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
            }
            extensions.configure<KotlinAndroidProjectExtension> {
                jvmToolchain(21)
            }
        }
    }
}
