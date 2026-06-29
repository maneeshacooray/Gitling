// Top-level build file — dependencies for subprojects go in their respective build.gradle.kts

// ASM is used by JGitCompatPlugin (app/build.gradle.kts) to patch JGit bytecode at build time,
// replacing InputStream.readNBytes(int)/transferTo() calls with Java-8-compatible shims so JGit
// 7.x works on Android < API 33. It must be on the buildscript classpath so app/build.gradle.kts
// can use it inline (AGP's AsmClassVisitorFactory requires the same classloader as AGP itself,
// which rules out buildSrc).
buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("org.ow2.asm:asm:9.7") }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false  // Kotlin 2.0+ Compose compiler

    // Dependency updates plugin
    alias(libs.plugins.versions) apply false
}

// Apply the versions plugin to the root project so the 'dependencyUpdates' task is available
apply(plugin = "com.github.ben-manes.versions")
