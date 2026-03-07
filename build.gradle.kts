// Top-level build file — dependencies for subprojects go in their respective build.gradle.kts
plugins {
    id("com.android.application") version "8.3.2" apply false
    id("com.android.library") version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false  // Kotlin 2.0+ Compose compiler
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
