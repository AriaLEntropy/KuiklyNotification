plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("maven-publish")
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
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "KuiklyNotification KMP module"
        homepage = "https://github.com/AriaLEntropy/KuiklyNotification"
        version = MavenConfig.VERSION
        ios.deploymentTarget = "12.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "KuiklyNotification"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kuikly 运行时由宿主提供，不打包进产物
                compileOnly("com.tencent.kuikly-open:core:${Version.getKuiklyVersion()}")
                compileOnly("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyVersion()}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "io.github.arialentropy.notification"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }
}

/**
 * 发布配置。
 *
 * - iOS 通过 CocoaPods 分发（`pod 'KuiklyNotificationIOS', :git => ..., :tag => ...`），
 *   Maven 侧只需「元数据 + Android 变体」，因此**不发布 Apple 目标**（也避免在 Windows/Linux 上编译 Apple 目标）。
 * - POM 元数据统一由 buildSrc 的 `Publishing.applyPom` 填充。
 */
publishing {
    publications.withType<org.gradle.api.publish.maven.MavenPublication>().configureEach {
        Publishing.applyPom(this, project)
    }
    // 根（元数据）publication 的 artifactId 与模块名一致；各目标 publication 由 KMP 自动派生后缀
    publications.named<org.gradle.api.publish.maven.MavenPublication>("kotlinMultiplatform") {
        artifactId = MavenConfig.KMP_ARTIFACT_ID
    }
    Publishing.registerRepositories(repositories, project)
}
