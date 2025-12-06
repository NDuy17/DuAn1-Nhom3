plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "fpl.manhph61584.duan1_nhom3_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "fpl.manhph61584.duan1_nhom3_app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // ⭐ MPAndroidChart (Biểu đồ)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ⭐ Thư viện cơ bản Android
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ⭐ Glide (load ảnh)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // ⭐ Retrofit + Gson (API)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // ⭐ Unit test
    testImplementation(libs.junit)

    // ⭐ Android test
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
