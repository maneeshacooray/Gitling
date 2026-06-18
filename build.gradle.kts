// Top-level build file — dependencies for subprojects go in their respective build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false  // Kotlin 2.0+ Compose compiler

    // Dependency updates plugin
    alias(libs.plugins.versions) apply false
}

// Apply the versions plugin to the root project so the 'dependencyUpdates' task is available
apply(plugin = "com.github.ben-manes.versions")
