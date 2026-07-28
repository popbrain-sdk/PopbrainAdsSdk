plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ads.popbrain.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {

    // Install Referrer API (IMPORTANT for attribution)
    implementation(libs.installreferrer)

    // NOTE: no HTTP library on purpose. The SDK makes two API calls, handled by the
    // platform's own HttpURLConnection (see PopbrainApiClient), and builds its JSON with the
    // framework's org.json. Adding OkHttp/Retrofit/Gson cost ~1.4 MB for no functional gain.
    //
    // Likewise no androidx.core / appcompat / material — this is a headless SDK with no UI,
    // and those pulled ~9 MB of transitive deps into every integrator's app.

    // Testing
    testImplementation(libs.junit)
}