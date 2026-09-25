plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "io.github.arialentropy.notification.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "io.github.arialentropy.notification.demo"
        minSdk = 21
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":KuiklyNotificationAndroid"))

    // core-render-android 已传递 appcompat / recyclerview / dynamicanimation
    implementation("com.tencent.kuikly-open:core-render-android:${Version.getKuiklyVersion()}")
}
