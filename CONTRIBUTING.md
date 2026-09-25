# Contributing

> 贡献前建议先读 [README.md](README.md)（[English](README.en.md)）了解组件定位与平台差异。

## 开发环境

| 项 | 版本 |
|---|---|
| JDK | 17（AGP 8.4 要求） |
| Kuikly | `2.24.0-2.1.21` |
| Kotlin | `2.1.21` |
| AGP / Gradle | `8.4.0` / `8.6` |
| iOS 构建 | macOS + Xcode + CocoaPods |
| 鸿蒙构建 | DevEco Studio（含 `hvigorw`）；跨端产物需 `OHOS_SDK_HOME` |

版本集中配置在 [`buildSrc/src/main/java/KotlinBuildVar.kt`](buildSrc/src/main/java/KotlinBuildVar.kt)，**改动版本请只改这一处**。

## 工程结构

```
KuiklyNotification/            Kuikly 侧（KMP）：Module、数据模型、常量、错误码
KuiklyNotificationAndroid/     Android 实现（AAR）
KuiklyNotificationIOS/         iOS 实现（源码 + podspec）
KuiklyNotificationOhos/        鸿蒙实现（ArkTS HAR）
shared/                        跨端 Demo 页面（@Page("router")）
androidApp/ iosApp/ ohosApp/   三端壳工程
buildSrc/                      版本、坐标、POM 元数据、发布仓库
maven-repo/                    已发布的 Maven 产物（推 gh-pages 即公开）
```

## 本地构建

```bash
# Android（任意 OS 均可编译）
./gradlew :androidApp:assembleDebug
./gradlew :KuiklyNotification:assemble :KuiklyNotificationAndroid:assemble

# iOS（macOS）
./gradlew :shared:generateDummyFramework
cd iosApp && pod install

# 鸿蒙 HAR
cd ohosApp && hvigorw assembleHar

# 鸿蒙跨端产物（Kotlin/Native 独立编译链，需 OHOS_SDK_HOME）
./gradlew -c settings.ohos.gradle.kts :shared:linkSharedDebugSharedOhosArm64
```

> 鸿蒙 App 运行限制：Kuikly 引擎仅 **arm64**，Windows / Intel Mac 的 x86_64 鸿蒙模拟器**跑不了 Kuikly**，
> 需鸿蒙真机或 Apple Silicon Mac 模拟器。x86 模拟器上可用 `ohosApp/entry` 做纯 ArkTS 通知验证。

## 新增一个能力

1. Kuikly 侧 `KuiklyNotification/src/commonMain` 加方法（`KRNotificationModule` 是具体类，不使用 expect/actual）
2. 三端各自实现：
   - Android：`KuiklyNotificationAndroid/.../KRNotificationModule.kt`
   - iOS：`KuiklyNotificationIOS/Classes/Module/KRNotificationModule.m`（按**方法名**反射分发）
   - 鸿蒙：`KuiklyNotificationOhos/src/main/ets/kuikly/KRNotificationModule.ets`（`call()` 分发）
3. **统一回调**：native 回包 `{"code","msg","data"}`，错误码复用 `NotificationConst.ErrorCode`
4. 端不支持的必须回调 `UNSUPPORTED(1010)`，不要静默失败
5. 平台差异登记到 README 的「平台差异与已知限制」
6. **中英双语同步更新** README，并在 [CHANGELOG.md](CHANGELOG.md) 记录

## 提交与 PR

- 一个提交一个关注点；能编译就先本地验证
- 语义化提交信息：`feat(android):` / `fix(ohos):` / `docs:` / `style(demo):` / `build:`
- 改了公共 API：必须同时更新 README 两语言 + CHANGELOG
- PR 描述里请写清：改了哪个端、怎么验证的、有无平台差异

PR checklist：

- [ ] `./gradlew :androidApp:assembleDebug` 通过
- [ ] 若改 iOS：在 Mac 上 `pod install` + 编译通过
- [ ] 若改鸿蒙：`cd ohosApp && hvigorw assembleHar` 通过
- [ ] README（中文 + English）已同步
- [ ] CHANGELOG 已记录

---

## Contributing (English summary)

- Read [README.en.md](README.en.md) first.
- JDK 17, Kuikly `2.24.0-2.1.21`, Kotlin `2.1.21`, AGP `8.4.0` / Gradle `8.6`. Versions live in
  [`buildSrc/src/main/java/KotlinBuildVar.kt`](buildSrc/src/main/java/KotlinBuildVar.kt) — change them there only.
- Add a capability by: adding the method on the Kuikly side, implementing it on all three platforms
  (iOS dispatches by selector name, HarmonyOS by `call()`), replying `{"code","msg","data"}` and
  `UNSUPPORTED(1010)` where unsupported.
- Always update **both languages of the README** and the [CHANGELOG.md](CHANGELOG.md).
- Conventional commits (`feat(android):`, `fix(ohos):`, `docs:`); one concern per commit; verify locally first.
- HarmonyOS note: the Kuikly engine is arm64-only, so Windows / Intel Mac x86_64 emulators cannot run Kuikly;
  use a real device or an Apple Silicon Mac emulator. `ohosApp/entry` works as a pure-ArkTS check on x86.
