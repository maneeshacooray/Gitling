plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)  // Kotlin 2.0+: Compose compiler is bundled with Kotlin
    id("kotlin-parcelize")
}

android {
    namespace = "me.sheimi.sgit"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.maneeshacooray.gitling"
        minSdk = 23
        targetSdk = 37
        versionCode = 16
        versionName = "1.0.15"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // Release signing is configured externally via project properties (see
        // .github/workflows/release.yml -- passed as -Pspecial -Palias=... etc).
        // Without them (e.g. local `assembleRelease`), the release build type falls
        // back to no signingConfig, same as any other unconfigured AGP release build.
        if (project.hasProperty("special")) {
            create("release") {
                keyAlias = project.property("alias") as String
                keyPassword = project.property("password") as String
                storeFile = file(project.property("keystore") as String)
                storePassword = project.property("store_password") as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Strong skipping is enabled by default since Kotlin 2.0.20; no composeCompiler block needed.
    // composeOptions.kotlinCompilerExtensionVersion is NOT used with Kotlin 2.0+.
    // The Compose compiler is bundled with Kotlin via the kotlin.plugin.compose plugin above.

    lint {
        abortOnError = false
    }

    packaging {
        resources {
            excludes += setOf("META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE")
            excludes += "META-INF/versions/**/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    // Desugaring for Java 8+ APIs on older Android
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.material.kolor)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.vectordrawable)
    implementation(libs.androidx.constraintlayout)

    // Compose BOM — manages all compose versions consistently
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // LiveData observe as Compose state
    implementation(libs.androidx.compose.runtime.livedata)

    implementation(libs.timber)
    implementation(libs.jsch)
    implementation(libs.commons.io)
    implementation(libs.jgit.core)
    implementation(libs.jgit.ssh.jsch) {
        exclude(group = "com.jcraft", module = "jsch")
    }
    implementation(libs.universal.image.loader)
    implementation(libs.secure.preferences)
    implementation(libs.conscrypt)
    implementation(libs.bcprov)

    implementation(libs.acra.mail)
    implementation(libs.acra.dialog)

    debugImplementation(libs.stetho.core)
    debugImplementation(libs.stetho.timber)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
