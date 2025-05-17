plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.maramagagriculturalaid.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.maramagagriculturalaid.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

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

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/NOTICE.md"
        }
    }
}

dependencies {
    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.multidex:multidex:2.0.1")

    // Firebase BOM for version management
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Google API Client for Android
    implementation("com.google.api-client:google-api-client-android:2.2.0") {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
    implementation("com.google.api-client:google-api-client-gson:2.2.0") {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // Google Drive API
    implementation("com.google.apis:google-api-services-drive:v3-rev20211107-1.32.1") {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // HTTP Client
    implementation("com.google.http-client:google-http-client-android:1.43.3") {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // OAuth Client
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.api-client:google-api-client:1.34.1")


    // Activity
    implementation(libs.activity)

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}