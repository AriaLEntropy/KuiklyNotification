// 鸿蒙(Kotlin/Native)构建脚本，仅在 -c settings.ohos.gradle.kts 时生效。
// 说明：鸿蒙端符号在链接期由 @kuikly-open/render 提供的 libkuikly.so 提供，
// 故这里只声明编译期依赖，运行期由宿主 HAP 装配。
plugins {
    kotlin("multiplatform")
}

version = MavenConfig.VERSION
group = MavenConfig.GROUP

kotlin {
    ohosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.tencent.kuikly-open:core:${Version.getKuiklyOhosVersion()}")
                api("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyOhosVersion()}")
            }
        }
    }
}
