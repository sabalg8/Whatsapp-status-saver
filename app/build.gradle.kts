plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.status_saver_app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.status_saver_app"
        minSdk = 19
        targetSdk = 34
        multiDexEnabled =true
        versionCode = 1
        versionName = "1.0"
        resourceConfigurations.addAll(listOf("en"))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {

            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding= true
    }
}

dependencies {
    implementation (libs.commons.io)
    implementation (libs.exoplayer.core)
    implementation (libs.exoplayer.ui)
    implementation (libs.zoomage)
    implementation (libs.androidx.multidex)
    implementation (libs.android.rate)
    implementation (libs.androidx.swiperefreshlayout)
    implementation (libs.glide)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.media3.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}