plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")  // Kotlin 2.0+: Compose compiler is bundled with Kotlin
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "me.sheimi.sgit"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    defaultConfig {
        applicationId = "com.manichord.mgit"
        minSdk = 21
        targetSdk = 35

        vectorDrawables.useSupportLibrary = true

        versionCode = 241
        versionName = "2.0.0-m3"
    }

    buildFeatures {
        dataBinding = true
        compose = true
        buildConfig = true  // AGP 8.x disabled this by default; needed for BuildConfig.DEBUG etc.
    }

    // composeOptions.kotlinCompilerExtensionVersion is NOT used with Kotlin 2.0+.
    // The Compose compiler is bundled with Kotlin via the kotlin.plugin.compose plugin above.

    lint {
        abortOnError = false
    }

    signingConfigs {
        // Release signing is configured externally via project properties
        if (project.hasProperty("special")) {
            create("release") {
                keyAlias = project.property("alias") as String
                keyPassword = project.property("password") as String
                storeFile = file(project.property("keystore") as String)
                storePassword = project.property("password") as String
            }
        }
    }

    buildTypes {
        release {
            if (project.hasProperty("special")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    packaging {
        resources {
            excludes += setOf("META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE")
        }
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.jcraft" && requested.name == "jsch") {
            useTarget("com.github.mwiede:jsch:0.2.0")
        }
    }
    exclude(module = "httpclient")
}

dependencies {
    // Desugaring for Java 8+ APIs on older Android
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("androidx.fragment:fragment:1.8.3")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.vectordrawable:vectordrawable:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Compose BOM — manages all compose versions consistently
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    kapt("androidx.lifecycle:lifecycle-compiler:2.8.6")

    // LiveData observe as Compose state
    implementation("androidx.compose.runtime:runtime-livedata")

    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.github.mwiede:jsch:0.2.0")
    implementation("commons-io:commons-io:2.21.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:3.7.1.201504261725-r")
    implementation("com.nostra13.universalimageloader:universal-image-loader:1.9.5")
    implementation("com.scottyab:secure-preferences-lib:0.1.7")
    implementation("org.conscrypt:conscrypt-android:2.5.3")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    val acraVersion = "5.13.1"
    implementation("ch.acra:acra-mail:$acraVersion")
    implementation("ch.acra:acra-dialog:$acraVersion")

    debugImplementation("com.facebook.stetho:stetho:1.6.0")
    debugImplementation("com.facebook.stetho:stetho-timber:1.6.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.1")
}
