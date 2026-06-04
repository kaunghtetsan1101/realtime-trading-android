// Root build file — module configuration lives in each module's build.gradle.kts.

// Hilt 2.59 bundles kotlin-metadata-jvm that tops out at metadata 2.3.0;
// Kotlin 2.4.0 generates metadata 2.4.0, so we force a compatible version.
allprojects {
    configurations.all {
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

// ── Static analysis (detekt) ─────────────────────────────────────────────────
// Scans all module sources in one pass. Run: ./gradlew detekt
// Config: config/detekt/detekt.yml  (overrides only; builds upon detekt defaults)
detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    source.setFrom(
        fileTree(rootDir) {
            include("**/src/main/kotlin/**/*.kt", "**/src/test/kotlin/**/*.kt")
            exclude("**/build/**")
        },
    )
}

// ── Formatting (Spotless + ktlint) ───────────────────────────────────────────
// Check: ./gradlew spotlessCheck
// Fix:   ./gradlew spotlessApply   ← run this once after initial setup
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint("1.5.0")
            .editorConfigOverride(
                mapOf(
                    // Composable functions use PascalCase by Jetpack Compose convention
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable,Preview",
                ),
            )
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**/*.kts")
        ktlint("1.5.0")
    }
}
