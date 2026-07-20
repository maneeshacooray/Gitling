plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)  // Kotlin 2.0+: Compose compiler is bundled with Kotlin
    id("kotlin-parcelize")
}

// ---------------------------------------------------------------------------
// JGit Android 12 compatibility — bytecode patch
//
// JGit 6.3+ uses InputStream.transferTo() (API 29) and JGit 6.7+ uses both
// InputStream.readNBytes(int) and InputStream.readNBytes(byte[], int, int)
// (API 33). None of these are available on Android 12 (API 31), and
// desugar_jdk_libs 2.x does not backport any of them.
//
// Solution: at build time, rewrite every INVOKEVIRTUAL call to those methods
// inside JGit classes into INVOKESTATIC calls to StreamCompat shims that are
// Java-8-compatible. The stack layout is identical for both opcodes (object
// reference is already first on the stack), so no extra bytecode is emitted.
// StreamCompat lives in me.sheimi.sgit.compat.
// ---------------------------------------------------------------------------

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

abstract class JGitCompatClassVisitorFactory :
    AsmClassVisitorFactory<InstrumentationParameters.None> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor = object : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {
        override fun visitMethod(
            access: Int, name: String, descriptor: String,
            signature: String?, exceptions: Array<String>?
        ): MethodVisitor = object : MethodVisitor(
            Opcodes.ASM9,
            super.visitMethod(access, name, descriptor, signature, exceptions)
        ) {
            override fun visitMethodInsn(
                opcode: Int, owner: String, name: String,
                descriptor: String, isInterface: Boolean
            ) {
                // owner is the *compile-time* static type of the receiver, which for calls made
                // through JGit's own InputStream subclasses (e.g. SilentFileInputStream) is that
                // subclass, not java/io/InputStream itself -- matching only the exact JDK owner
                // silently left those call sites unpatched (see JGit-on-old-Android crash where
                // IO.readFully(File, int) calls SilentFileInputStream.readNBytes(int) directly).
                // isInstrumentable() below already restricts this whole visitor to
                // org.eclipse.jgit.* classes, so it's safe to match by name+descriptor across any
                // owner in that scope rather than requiring java/io/InputStream exactly.
                if (opcode == Opcodes.INVOKEVIRTUAL &&
                    (owner == "java/io/InputStream" || owner.startsWith("org/eclipse/jgit/"))
                ) {
                    when {
                        name == "readNBytes" && descriptor == "(I)[B" -> {
                            super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "me/sheimi/sgit/compat/StreamCompat",
                                "readNBytes",
                                "(Ljava/io/InputStream;I)[B",
                                false
                            )
                            return
                        }
                        name == "readNBytes" && descriptor == "([BII)I" -> {
                            super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "me/sheimi/sgit/compat/StreamCompat",
                                "readNBytes",
                                "(Ljava/io/InputStream;[BII)I",
                                false
                            )
                            return
                        }
                        name == "transferTo" && descriptor == "(Ljava/io/OutputStream;)J" -> {
                            super.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "me/sheimi/sgit/compat/StreamCompat",
                                "transferTo",
                                "(Ljava/io/InputStream;Ljava/io/OutputStream;)J",
                                false
                            )
                            return
                        }
                    }
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            }
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean =
        classData.className.startsWith("org.eclipse.jgit.")
}

androidComponents {
    onVariants { variant ->
        variant.instrumentation.transformClassesWith(
            JGitCompatClassVisitorFactory::class.java,
            InstrumentationScope.ALL
        ) {}
        variant.instrumentation.setAsmFramesComputationMode(
            FramesComputationMode.COPY_FRAMES
        )
    }
}

android {
    namespace = "me.sheimi.sgit"
    compileSdk = 37

    // AGP embeds an extra "Dependency metadata" block in the release APK's signing block by
    // default (for Google Play's SDK Console/dependency-transparency feature) -- F-Droid's
    // "check apk" verification explicitly scans for and rejects any such extra signing block,
    // since it's not something derivable from the source the way the APK's actual content is.
    // Only disabled for the raw APK (what GitHub releases and F-Droid build from) -- left enabled
    // for the AAB (includeInBundle stays at its default) so a future Play Store upload still
    // gets the full dependency-transparency metadata Google Play actually uses it for.
    dependenciesInfo {
        includeInApk = false
    }

    defaultConfig {
        applicationId = "com.maneeshacooray.gitling"
        minSdk = 23
        targetSdk = 37
        versionCode = 53
        versionName = "1.0.53"

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
            excludes += "plugin.properties"
        }
        // This app has no native source of its own -- all .so files come prebuilt from
        // dependencies (e.g. Conscrypt). AGP's debug-symbol stripping for those runs the NDK's
        // llvm-strip, and which NDK version gets resolved for that isn't pinned anywhere, so it
        // can (and does) differ across build machines, producing a non-byte-identical .so even
        // though the underlying dependency artifact is identical -- this broke F-Droid's
        // reproducible-build verification (libconscrypt_jni.so differed for all 4 ABIs between
        // GitHub Actions' build and F-Droid's, even after the JDK was made to match). Disabling
        // stripping makes these libraries a straight, environment-independent copy instead.
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
}

// Pins the exact JDK Gradle uses to compile, regardless of whatever JDK is installed on the
// host machine running the build -- compileOptions above only sets the *output bytecode level*
// (sourceCompatibility/targetCompatibility), not which JDK actually runs javac/kotlinc/D8. Without
// this, two CI environments with different host JDKs can produce non-byte-identical APKs from
// the same source (different D8 dexing output, different java.util.zip/DEFLATE compression
// behavior for the same input bytes) even though both builds are otherwise correct -- this is
// what broke F-Droid's reproducible-build verification.
//
// Pinned to 21, not 17, specifically because F-Droid's build container already has JDK 21
// preinstalled and its build policy's "suss" scanner hard-blocks the foojay-resolver plugin
// (org.gradle.toolchains.foojay-resolver-convention) needed to *auto-download* a missing JDK --
// F-Droid's builds aren't allowed to fetch extra toolchains over the network mid-build. Pinning
// to a version already present on both sides (GitHub Actions' release workflow now also
// installs Temurin 21, see .github/workflows/release.yml) means plain toolchain auto-detection
// (no downloading, no foojay) satisfies this on both CI environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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

    // Home-screen widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

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
