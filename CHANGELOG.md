# Changelog

所有值得记录的变更都写在这里。
格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [SemVer](https://semver.org/lang/zh-CN/)。

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0] - 2026-09-25

### Added

- 跨端本地通知组件（Android / iOS / 鸿蒙），Kuikly 侧统一 API `KRNotificationModule`
  / Cross-platform local notification component (Android / iOS / HarmonyOS) with a unified Kuikly-side API.
- 能力：权限申请/查询、通知渠道、立即发送、定时、重复、取消/取消全部、点击回跳（常驻监听）、冷启动 payload、角标（iOS）
  / Capabilities: permission request/check, channels, immediate, scheduled, repeating, cancel/cancelAll,
  tap-to-open (persistent listener), cold-start payload and badges (iOS).
- 厂商适配与设置引导 API：`isBatteryOptimizationEnabled` / `openBatteryOptimizationSettings` /
  `openAutoStartSettings` / `openNotificationSettings`（Android 4 个；鸿蒙/iOS 仅 `openNotificationSettings`）
  / Vendor settings-guidance APIs (all four on Android; `openNotificationSettings` only on HarmonyOS/iOS).
- 三端 Demo：`androidApp` / `iosApp` / `ohosApp`，以及可在 x86 鸿蒙模拟器运行的纯 ArkTS 验证 Demo（`ohosApp/entry`）
  / Demos for all three platforms, plus a pure-ArkTS verification demo (`ohosApp/entry`) runnable on x86 emulators.
- 鸿蒙 Kotlin/Native 独立编译链（`settings.ohos.gradle.kts`）
  / Separate HarmonyOS Kotlin/Native build chain.
- Maven 发布配置（`maven-publish`，产物落到 `maven-repo/`，可推送到 `gh-pages` 作为公开 Maven 仓库）
  / Maven publishing (artifacts in `maven-repo/`, pushable to `gh-pages` as a public Maven repository).
- 鸿蒙 ohpm 发布准备：包元数据 / 包内 README 与 LICENSE / `ohpm prepublish` 预校验通过 / `publish-ohpm.sh`
  （待注册 OpenHarmony 三方库中心仓账号后即可发布）
  / HarmonyOS ohpm publishing prepared (package metadata, packaged README/LICENSE, passing `ohpm prepublish`,
  `publish-ohpm.sh`); only an OpenHarmony registry account is pending.

### Documented

- 实测结论：鸿蒙横幅需系统「横幅通知」开关（默认关闭）；代理提醒需 `reminder_capability` 资质且提前量 ≥30 秒；
  Kuikly 鸿蒙引擎仅 arm64，Windows / Intel Mac 的 x86_64 模拟器无法运行
  / Verified constraints above are documented in the README.
- 中英双语 README 与 `CONTRIBUTING.md`
  / Bilingual README and CONTRIBUTING.

[Unreleased]: https://github.com/AriaLEntropy/KuiklyNotification/commits/main
[1.0.0]: https://github.com/AriaLEntropy/KuiklyNotification/releases/tag/1.0.0
