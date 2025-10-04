plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.heyu.zhudeapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.heyu.zhudeapp"
        minSdk = 24
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

// Add this block to force a specific version of the browser library
configurations.all {
    resolutionStrategy {
        force("androidx.browser:browser:1.8.0")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    // Replaced libs alias with a direct dependency to ensure it resolves correctly.
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.glide)//Glide
    implementation(libs.toasty)//Toasty
    implementation(libs.lottie)//lottie

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.5.3"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // Ktor - Explicitly use a version compatible with Supabase BOM 2.5.3
    implementation("io.ktor:ktor-client-android:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")

    implementation(libs.photoview)

}
