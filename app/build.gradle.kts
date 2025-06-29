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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.play.services.auth)
    implementation(libs.glide)
    implementation(libs.circleimageview)
    implementation("com.cloudinary:cloudinary-android:2.4.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}