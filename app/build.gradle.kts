plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.bgremover.vqzpt"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD") ?: "bgwrap123"
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD") ?: "bgwrap123"
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("generateReleaseKeystore") {
    doLast {
        val keystoreFile = file("${rootDir}/my-upload-key.jks")
        if (!keystoreFile.exists()) {
            println("Generating release keystore at: ${keystoreFile.absolutePath}")
            val cmd = listOf(
                "keytool", "-genkeypair",
                "-alias", "upload",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-keystore", keystoreFile.absolutePath,
                "-storepass", "bgwrap123",
                "-keypass", "bgwrap123",
                "-dname", "CN=BGWrap, OU=AIStudio, O=Google, L=MountainView, S=California, C=US"
            )
            println("Running command: ${cmd.joinToString(" ")}")
            val process = ProcessBuilder(cmd).start()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                println("Release keystore generated successfully!")
            } else {
                val errorStream = process.errorStream.bufferedReader().readText()
                println("Error generating keystore. Exit code: $exitCode. Error: $errorStream")
            }
        } else {
            println("Release keystore already exists.")
        }
    }
}

tasks.register("copyReleaseBinaries") {
    dependsOn("assembleRelease", "bundleRelease", "assembleDebug")
    doLast {
        val apkFile = file("${project.layout.buildDirectory.get().asFile}/outputs/apk/release/app-release.apk")
        val aabFile = file("${project.layout.buildDirectory.get().asFile}/outputs/bundle/release/app-release.aab")
        val debugApkFile = file("${project.layout.buildDirectory.get().asFile}/outputs/apk/debug/app-debug.apk")
        
        // Target absolute workspace /.build-outputs/ directory in rootDir
        val buildOutputsDir = file("${rootDir}/.build-outputs")
        if (!buildOutputsDir.exists()) {
            buildOutputsDir.mkdirs()
        }

        val apkDestBuildOutputs = file("${rootDir}/.build-outputs/release.apk")
        val aabDestBuildOutputs = file("${rootDir}/.build-outputs/release.aab")
        val debugApkDestBuildOutputs = file("${rootDir}/.build-outputs/app-debug.apk")
        
        fun printSize(f: java.io.File, name: String) {
            val sizeInMb = f.length().toDouble() / (1024 * 1024)
            println("FILE INFO: $name is %.2f MB (${f.length()} bytes) at ${f.absolutePath}")
        }
        
        if (apkFile.exists()) {
            apkFile.copyTo(apkDestBuildOutputs, overwrite = true)
            println("SUCCESS: Copied release APK to ${rootDir}/.build-outputs/release.apk")
            printSize(apkDestBuildOutputs, "apkDestBuildOutputs")
        } else {
            println("WARNING: Release APK not found at: ${apkFile.absolutePath}")
        }
        
        if (aabFile.exists()) {
            aabFile.copyTo(aabDestBuildOutputs, overwrite = true)
            println("SUCCESS: Copied release AAB to ${rootDir}/.build-outputs/release.aab")
            printSize(aabDestBuildOutputs, "aabDestBuildOutputs")
        } else {
            println("WARNING: Release AAB not found at: ${aabFile.absolutePath}")
        }

        if (debugApkFile.exists()) {
            debugApkFile.copyTo(debugApkDestBuildOutputs, overwrite = true)
            println("SUCCESS: Copied debug APK to ${rootDir}/.build-outputs/app-debug.apk")
            printSize(debugApkDestBuildOutputs, "debugApkDestBuildOutputs")
        } else {
            println("WARNING: Debug APK not found at: ${debugApkFile.absolutePath}")
        }
    }
}


