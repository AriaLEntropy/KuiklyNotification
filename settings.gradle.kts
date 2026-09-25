pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        // 公开只读镜像：仅用于下载 Kuikly 等构件，无需任何账号；本库不向该仓库上传
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        // 公开只读镜像：仅用于下载 Kuikly 等构件，无需任何账号；本库不向该仓库上传
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

rootProject.name = "KuiklyNotification"

include(":KuiklyNotification")
include(":KuiklyNotificationAndroid")
