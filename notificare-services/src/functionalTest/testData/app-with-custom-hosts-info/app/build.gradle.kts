plugins {
    // No version! One way or the other, the versions comes from the root project
    id("com.android.application")
    id("re.notifica.gradle.notificare-services")
}

android {
    namespace = "com.example.app"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
