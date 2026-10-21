plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// The release key never enters the repository: its keystore and password are Gradle properties
// (kept in ~/.gradle/gradle.properties) or environment variables. See README, "Release signing".
fun releaseSigning(property: String, variable: String): String? =
    providers.gradleProperty(property).orElse(providers.environmentVariable(variable)).orNull

val releaseKeystore = releaseSigning("perAnkhKeystore", "PER_ANKH_KEYSTORE")

// The version, and a version code derived from it so it always rises: 1.2.3 is 10203. Release
// steps are in RELEASING.md.
val appVersion = "0.2.0"
val appVersionCode = appVersion.split(".").map(String::toInt)
    .let { (major, minor, patch) -> major * 10_000 + minor * 100 + patch }

android {
    namespace = "com.krbavcic.pentalingo"
    compileSdk = 36

    defaultConfig {
        // Permanent once on Google Play. Named after hrana.app, like Hrana's own app.hrana.twa.
        applicationId = "app.hrana.perankh"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion

        buildConfigField("String", "REPORTS_URL", "\"https://perankh.hrana.app/v1/reports\"")
        buildConfigField("String", "PRIVACY_URL", "\"https://perankh.hrana.app/privacy\"")
    }

    signingConfigs {
        create("release") {
            storeFile = releaseKeystore?.let { file(it) }
            storePassword = releaseSigning("perAnkhKeystorePassword", "PER_ANKH_KEYSTORE_PASSWORD")
            keyAlias = "per-ankh"
            // keytool's default PKCS12 keystore has one password for the store and its key.
            keyPassword =
                releaseSigning("perAnkhKeyPassword", "PER_ANKH_KEY_PASSWORD") ?: storePassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                // So the project still builds anywhere; but Android installs an update only over
                // an app signed with the same key, so this APK cannot update a published release.
                logger.warn(
                    "Per Ankh: no release key (perAnkhKeystore is not set; see README, \"Release " +
                        "signing\"). The release APK is signed with the debug key and will not " +
                        "install over a published release."
                )
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        // The clips are already MP3; compressing them again only costs time at every play.
        noCompress += "mp3"
    }
}

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-engine"))
    implementation(project(":content"))
    implementation(project(":data"))
    implementation(project(":core-ui"))
    implementation(project(":feature-path"))
    implementation(project(":feature-lesson"))
    implementation(project(":feature-codex"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
