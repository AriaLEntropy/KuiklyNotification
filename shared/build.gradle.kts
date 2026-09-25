plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    // Kuikly 编译器插件：处理 @Page 注解，生成页面注册表与各端入口类（iOS 需 KuiklyCoreEntry）
    id("com.tencent.kuikly-open.kuikly")
}

version = MavenConfig.VERSION
group = MavenConfig.GROUP

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "KuiklyNotification cross-platform demo page"
        homepage = "https://github.com/AriaLEntropy/KuiklyNotification"
        version = MavenConfig.VERSION
        ios.deploymentTarget = "12.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.tencent.kuikly-open:core:${Version.getKuiklyVersion()}")
                implementation("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyVersion()}")
                implementation(project(":KuiklyNotification"))
            }
        }
    }
}

android {
    namespace = "io.github.arialentropy.notification.demo"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}
