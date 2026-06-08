import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(21)
            }
            dependencies {
                "implementation"(catalog.findLibrary("coroutines-core").get())
                "testImplementation"(catalog.findLibrary("junit4").get())
                "testImplementation"(catalog.findLibrary("coroutines-test").get())
                "testImplementation"(catalog.findLibrary("mockk").get())
                "testImplementation"(catalog.findLibrary("turbine").get())
            }
        }
    }
}
