import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("tradingapp.android.library.compose")
            pluginManager.apply("tradingapp.android.hilt")
            dependencies {
                "implementation"(catalog.findLibrary("androidx-lifecycle-runtime-ktx").get())
                "implementation"(platform(catalog.findLibrary("compose-bom").get()))
                "implementation"(catalog.findLibrary("compose-ui").get())
                "implementation"(catalog.findLibrary("compose-ui-graphics").get())
                "implementation"(catalog.findLibrary("compose-ui-tooling-preview").get())
                "implementation"(catalog.findLibrary("compose-material3").get())
                "implementation"(catalog.findLibrary("compose-material-icons-extended").get())
                "implementation"(catalog.findLibrary("hilt-lifecycle-viewmodel-compose").get())
                "implementation"(catalog.findLibrary("coroutines-core").get())
                "implementation"(catalog.findLibrary("coroutines-android").get())
                "implementation"(catalog.findLibrary("timber").get())
                "debugImplementation"(catalog.findLibrary("compose-ui-tooling").get())
                "testImplementation"(catalog.findLibrary("junit4").get())
                "testImplementation"(catalog.findLibrary("coroutines-test").get())
                "testImplementation"(catalog.findLibrary("turbine").get())
                "testImplementation"(catalog.findLibrary("mockk").get())
                "androidTestImplementation"(platform(catalog.findLibrary("compose-bom").get()))
                "androidTestImplementation"(catalog.findLibrary("compose-ui-test-junit4").get())
            }
        }
    }
}
