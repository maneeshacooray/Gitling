pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Lets Gradle auto-download the pinned JDK 17 toolchain (app/build.gradle.kts) when the host
// machine doesn't already have it installed -- without this, "Toolchain auto-provisioning is
// not enabled" build failures occur on any machine without JDK 17 present, e.g. F-Droid's build
// container (which only has JDK 21).
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack for jsch fork
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MGit"
include(":app")
