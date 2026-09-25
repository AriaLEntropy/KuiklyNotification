// 鸿蒙(Kotlin/Native)构建脚本，仅在 -c settings.ohos.gradle.kts 时生效。
// 产出：shared/build/bin/ohosArm64/debugShared/libshared.so + libshared_api.h
//       （由 entry 的 CMake 链接进 kuikly_entry.so）
plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

version = MavenConfig.VERSION
group = MavenConfig.GROUP

kotlin {
    ohosArm64 {
        binaries {
            // 业务动态库，命名 libshared.so，与 ohosApp/entry 的 CMakeLists 约定一致
            sharedLib("shared")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.tencent.kuikly-open:core:${Version.getKuiklyOhosVersion()}")
                implementation("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyOhosVersion()}")
                implementation(project(":KuiklyNotification"))
            }
        }
    }
}

dependencies {
    // 鸿蒙端的页面/入口注册由 KSP 生成（core-ksp 内的 OhOsTargetEntryBuilder）
    add("kspOhosArm64", "com.tencent.kuikly-open:core-ksp:${Version.getKuiklyOhosVersion()}")
}

ksp {
    arg("catchException", "false")
}
