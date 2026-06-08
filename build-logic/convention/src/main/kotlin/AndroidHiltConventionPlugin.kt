import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.dagger.hilt.android")
            pluginManager.apply("com.google.devtools.ksp")
            dependencies {
                "implementation"(catalog.findLibrary("hilt-android").get())
                "ksp"(catalog.findLibrary("hilt-compiler").get())
            }
        }
    }
}
