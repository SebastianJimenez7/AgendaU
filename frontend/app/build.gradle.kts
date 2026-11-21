import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// La clave y el modelo se leen de local.properties (ignorado por Git)
val propiedadesLocales = Properties()
val archivoLocal = rootProject.file("local.properties")
if (archivoLocal.exists()) {
    archivoLocal.inputStream().use { propiedadesLocales.load(it) }
}
val claveIa = propiedadesLocales.getProperty("GEMINI_API_KEY", "")
val modeloIa = propiedadesLocales.getProperty("GEMINI_MODEL", "gemini-3.8-flash")

android {
    namespace = "com.agendau.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.agendau.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GEMINI_API_KEY", "\"$claveIa\"")
        buildConfigField("String", "GEMINI_MODEL", "\"$modeloIa\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
