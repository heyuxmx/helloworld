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

    signingConfigs {
        create("release") {
            val releaseStoreFile = project.findProperty("RELEASE_STORE_FILE")
            if (releaseStoreFile != null) {
                storeFile = file(releaseStoreFile)
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    defaultConfig {
        applicationId = "com.heyu.zhudeapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // 1. Define a flavor dimension to group your flavors
    flavorDimensions += "version"

    // 2. Define your two product flavors
    productFlavors {
        create("xiaogao") {
            dimension = "version"
            buildConfigField("String", "AVATAR_IDENTIFIER", "\"ic_launcher\"")
            buildConfigField("String", "USER_ID", "\"12345\"")
            buildConfigField("String", "UPDATE_JSON_URL", "\"https://bvgtzgxscnqhugjirgzp.supabase.co/storage/v1/object/public/app-releases/update-check_xiaogao.json\"")
        }
        
        create("xiaoxu") {
            dimension = "version"
            buildConfigField("String", "AVATAR_IDENTIFIER", "\"ic_launcher\"")
            buildConfigField("String", "USER_ID", "\"67890\"")
            buildConfigField("String", "UPDATE_JSON_URL", "\"https://bvgtzgxscnqhugjirgzp.supabase.co/storage/v1/object/public/app-releases/update-check_xiaoxu.json\"")
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
    implementation("com.vanniktech:android-image-cropper:4.5.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Android Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

}
