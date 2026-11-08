plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Ký release: CI truyền keystore qua biến môi trường — CÙNG TÊN secrets với
// KaraokeMaker Android (KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS /
// KEY_PASSWORD, xem WORKFLOW.txt). Không có biến môi trường (build tay) ->
// lùi về debug signing để vẫn ra APK cài thử.
val ksFile = System.getenv("KEYSTORE_FILE")
val releaseSigning = ksFile != null && file(ksFile).exists()

android {
    namespace = "com.example.museteam"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.museteam"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        if (releaseSigning) {
            create("release") {
                storeFile = file(ksFile!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // v1 ưu tiên ỔN ĐỊNH: không minify, không shrink.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (releaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
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
    // Chỉ appcompat — JSON dùng org.json có sẵn trong Android, tải mạng bằng
    // HttpURLConnection. Không thêm thư viện ngoài nào khác.
    implementation("androidx.appcompat:appcompat:1.7.0")
}
