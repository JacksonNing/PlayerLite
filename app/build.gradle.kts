plugins {
    id("playerlite.android.application")
    id("playerlite.android.compose")
}

val apiBaseUrl = providers.gradleProperty("playerlite.apiBaseUrl")
    .orElse("http://139.9.223.233:3000")
    .get()
val appVersionName = providers.gradleProperty("playerlite.versionName")
    .orElse("0.2.0")
    .get()
val appVersionCode = providers.gradleProperty("playerlite.versionCode")
    .map { it.toInt() }
    .orElse(2000)
    .get()
val releaseStoreFile = providers.environmentVariable("PLAYERLITE_RELEASE_STORE_FILE")
val releaseStorePassword = providers.environmentVariable("PLAYERLITE_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("PLAYERLITE_RELEASE_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("PLAYERLITE_RELEASE_KEY_PASSWORD")
val releaseSigningValues = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val hasReleaseSigningConfig = releaseSigningValues.all { it.isPresent }

require(releaseSigningValues.none { it.isPresent } || hasReleaseSigningConfig) {
    "Release signing configuration is incomplete"
}

android {
    namespace = "com.wxy.playerlite"
    ndkVersion = "27.0.12077973"
    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    defaultConfig {
        applicationId = "com.wxy.playerlite"
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }
}

dependencies {
    implementation(project(":design-system"))
    implementation(project(":feature-discovery"))
    implementation(project(":feature-player"))
    implementation(project(":feature-details"))
    implementation(project(":core-data"))
    implementation(project(":playback-api"))
    implementation(project(":playback-service"))
    implementation(project(":player"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.media3.session)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation("androidx.compose.ui:ui-test")
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.12.2")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
