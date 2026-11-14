// Build file for the app module: Android settings plus the libraries this app uses.
// AGP 9 compiles Kotlin by itself ("built-in Kotlin"), so no separate Kotlin Android plugin is needed.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose) // Compose compiler: turns @Composable functions into UI code
    alias(libs.plugins.ksp) // Kotlin Symbol Processing: runs code generators such as Room's compiler
    alias(libs.plugins.kotlin.serialization) // @Serializable: navigation routes and JSON DTOs
}

android {
    namespace = "com.portfoliox"
    compileSdk = 37 // Android API level we compile against (the newest APIs we can call)

    defaultConfig {
        applicationId = "com.portfoliox" // Unique ID of the app on a device and in the Play Store
        minSdk = 26 // Android 8.0: oldest version the app installs on
        targetSdk = 37 // Version whose behaviour the app is tested against
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Which market data source to use, read from gradle.properties at build time and exposed
        // to Kotlin as BuildConfig constants. "sample" works offline; "remote" calls the FastAPI server.
        val marketDataSource = providers.gradleProperty("portfoliox.marketDataSource").getOrElse("sample")
        val apiBaseUrl = providers.gradleProperty("portfoliox.apiBaseUrl").getOrElse("http://localhost:8000/")
        buildConfigField("String", "MARKET_DATA_SOURCE", "\"$marketDataSource\"")
        buildConfigField("String", "MARKET_API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true // generates the BuildConfig class with the fields above
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
