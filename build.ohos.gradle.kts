// 鸿蒙(Kotlin/Native ohosArm64)根构建脚本，仅通过 -c settings.ohos.gradle.kts 生效。
// Kotlin 使用腾讯定制工具链：官方版 Kotlin 不支持 ohosArm64 目标。
plugins {
    kotlin("multiplatform") version "2.0.21-KBA-010" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}
