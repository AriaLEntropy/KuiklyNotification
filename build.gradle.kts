buildscript {
    dependencies {
        classpath(BuildPlugin.kuikly)
    }
}

plugins {
    // 统一各子模块的插件版本
    id("com.android.application").version("8.5.1").apply(false)
    id("com.android.library").version("8.5.1").apply(false)
    kotlin("android").version("2.1.21").apply(false)
    kotlin("multiplatform").version("2.1.21").apply(false)
}
