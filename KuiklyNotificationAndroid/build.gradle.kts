plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "io.github.arialentropy.notification.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
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
    // Kuikly 渲染层（其传递依赖已包含 appcompat / core / fragment / recyclerview）
    api("com.tencent.kuikly-open:core-render-android:${Version.getKuiklyVersion()}")
    implementation("androidx.appcompat:appcompat:1.2.0")
    // 前后台判断（ProcessLifecycleOwner）
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
}
