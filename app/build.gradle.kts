plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.services)
}

// Tải khóa Cloudinary từ file local.properties
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = mutableMapOf<String, String>()
if (localPropertiesFile.exists()) {
    localPropertiesFile.reader().useLines { lines ->
        lines.forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
                val (key, value) = line.split("=", limit = 2).map { it.trim() }
                localProperties[key] = value
            }
        }
    }
}

// Đọc các khóa với giá trị dự phòng trong trường hợp null
val cloudName = localProperties["cloudinary_cloud_name"] ?: ""
val apiKey = localProperties["cloudinary_api_key"] ?: ""
val apiSecret = localProperties["cloudinary_api_secret"] ?: ""

android {
    namespace = "com.example.baicuoiky04"
    compileSdk = 34

    // Bật tính năng BuildConfig
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.baicuoiky04"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Định nghĩa các trường BuildConfig
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudName\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"$apiKey\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"$apiSecret\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // AndroidX & Material
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase - Dùng BOM để quản lý phiên bản
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // UI
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Third Party
    implementation("com.cloudinary:cloudinary-android:2.4.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.emoji2:emoji2:1.4.0")
    implementation("com.vanniktech:emoji-google:0.15.0")

    implementation("com.google.firebase:firebase-messaging")

}