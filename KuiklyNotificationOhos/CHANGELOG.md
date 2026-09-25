# Changelog

本包为 [KuiklyNotification](https://github.com/AriaLEntropy/KuiklyNotification) 的鸿蒙实现，
完整变更记录以仓库根目录的 [CHANGELOG.md](https://github.com/AriaLEntropy/KuiklyNotification/blob/main/CHANGELOG.md) 为准。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [SemVer](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2026-09-25

### Added

- 首个版本：鸿蒙本地通知实现 `KRNotificationModule`（ArkTS HAR），与 Android / iOS 共享同一套 Kuikly 侧 API。
- 能力：权限申请/查询、通知渠道（Slot）、立即发送、定时、重复、取消/取消全部、点击回跳、冷启动 payload。
- 设置引导：`openNotificationSettings`（拉起通知管理半模态，可用于引导开启「横幅通知」）。
- 已知限制（文档已说明）：
  - 定时 / 重复通知依赖代理提醒，需应用具备 `reminder_capability`，且提前量 ≥30 秒；
  - 横幅通知需系统设置里开启「横幅通知」（默认关闭）；
  - 通知渠道等级由 SlotType 固定（`HIGH→SOCIAL_COMMUNICATION` 等）。

[1.0.0]: https://github.com/AriaLEntropy/KuiklyNotification/releases/tag/1.0.0
