plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize") // Add this line
    // Apply the Google Services plugin
    id("com.google.gms.google-services")
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

    // 1. Define a flavor dimension to group your flavors
    flavorDimensions += "version"

    // 2. Define your two product flavors
    productFlavors {
        create("xiaogao") {
            dimension = "version"
            buildConfigField("String", "USERNAME", "\"小高宝宝\"")
            buildConfigField("String", "AVATAR_IDENTIFIER", "\"ic_launcher\"")
            buildConfigField("String", "USER_ID", "\"user_xiaogao\"")
        }
        
        create("xiaoxu") {
            dimension = "version"
            buildConfigField("String", "USERNAME", "\"小徐爸爸\"")
            buildConfigField("String", "AVATAR_IDENTIFIER", "\"ic_launcher\"")
            buildConfigField("String", "USER_ID", "\"user_xiaoxu\"")
        }
    }

    compileOptions {
        // ADD THIS LINE TO ENABLE CORE LIBRARY DESUGARING
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

// Add this block to force a specific version of the browser library
configurations.all {
    resolutionStrategy {
        force("androidx.browser:browser:1.8.0")
    }
}

dependencies {

    // ADD THIS DEPENDENCY FOR CORE LIBRARY DESUGARING
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(libs.androidx.core.ktx)
    // Replaced libs alias with a direct dependency to ensure it resolves correctly.
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.glide)//Glide
    implementation(libs.toasty)//Toasty
    implementation(libs.lottie)//lottie

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-common-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

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
