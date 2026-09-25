// 鸿蒙(Kotlin/Native ohosArm64)独立编译链设置。
// 用法：./gradlew -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64
//
// 为什么单独一份：鸿蒙产物需要腾讯定制版 Kotlin 工具链(KBA/KBAF)与 -ohos 后缀的 Kuikly 构件，
// 与 Android/iOS 使用的 Kotlin 2.1.21 不兼容，故用独立 settings 隔离。
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        // 定制版 Kotlin 工具链 + Kuikly 构件
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
        // 部分插件 marker（如 KSP）
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

rootProject.name = "KuiklyNotification"
rootProject.buildFileName = "build.ohos.gradle.kts"

include(":KuiklyNotification")
project(":KuiklyNotification").buildFileName = "build.ohos.gradle.kts"

include(":shared")
project(":shared").buildFileName = "build.ohos.gradle.kts"
