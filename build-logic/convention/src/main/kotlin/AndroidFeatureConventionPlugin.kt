import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("tradingapp.android.library.compose")
            pluginManager.apply("tradingapp.android.hilt")
            dependencies {
                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                "implementation"(platform(libs.findLibrary("compose-bom").get()))
                "implementation"(libs.findLibrary("compose-ui").get())
                "implementation"(libs.findLibrary("compose-ui-graphics").get())
                "implementation"(libs.findLibrary("compose-ui-tooling-preview").get())
                "implementation"(libs.findLibrary("compose-material3").get())
                "implementation"(libs.findLibrary("compose-material-icons-extended").get())
                "implementation"(libs.findLibrary("hilt-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("coroutines-core").get())
                "implementation"(libs.findLibrary("coroutines-android").get())
                "implementation"(libs.findLibrary("timber").get())
                "debugImplementation"(libs.findLibrary("compose-ui-tooling").get())
                "testImplementation"(libs.findLibrary("junit4").get())
                "testImplementation"(libs.findLibrary("coroutines-test").get())
                "testImplementation"(libs.findLibrary("turbine").get())
                "testImplementation"(libs.findLibrary("mockk").get())
                "androidTestImplementation"(platform(libs.findLibrary("compose-bom").get()))
                "androidTestImplementation"(libs.findLibrary("compose-ui-test-junit4").get())
            }
        }
    }
}
