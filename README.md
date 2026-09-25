# KuiklyNotification

基于 [Kuikly](https://github.com/Tencent-TDS/KuiklyUI) 的跨端本地通知组件（Android / iOS / 鸿蒙）。

提供统一入口在系统通知栏弹出消息，并支持点击回跳、冷启动携带 payload、定时/重复通知、权限与渠道管理。

## 能力

- 权限：申请 / 查询通知权限
- 渠道：Android 渠道、鸿蒙 Slot
- 通知：立即发送 / 取消 / 全部取消
- 定时 / 重复通知
- 点击事件（常驻监听）
- 冷启动拉起 payload
- 角标（iOS）

## 工程结构

```
KuiklyNotification/            Kuikly 侧（KMP）：Module、数据模型
KuiklyNotificationAndroid/     Android 实现
KuiklyNotificationIOS/         iOS 实现
KuiklyNotificationOhos/        鸿蒙实现
shared/                        跨端 Demo
androidApp/ iosApp/ ohosApp/   壳工程
```

## 使用

```kotlin
val module = acquireModule<KRNotificationModule>(KRNotificationModule.MODULE_NAME)
module.requestPermission { result -> /* result.code == 0 表示成功 */ }
module.show(NotificationRequest(id = 1, title = "标题", body = "内容")) { /* ... */ }
```

## 权限说明（Android）

- **Android 12（API 31）及以下**：系统没有通知运行时权限，`requestPermission` 直接返回 `GRANTED`，**不会弹窗**。
- **Android 13（API 33）及以上**：首次调用 `requestPermission` 弹系统授权框；用户**允许/拒绝之后，再次调用不会再弹**（SDK 内部拦截，直接返回 `GRANTED` / `DENIED`）。需要重新授权时请引导用户去系统设置。
- `checkPermission` 在“未申请过且未授权”时返回 `NOT_DETERMINED`。
- 举例：在 API 31 模拟器上点“请求权限”没有弹窗，属正常现象。

## 横幅（Heads-up）

Android 横幅只对 `IMPORTANCE_HIGH` 的渠道生效，且**渠道创建后重要性不可修改**。要出横幅：

1. 先用 `createChannel(channelId, importance = Importance.HIGH)` 建一个 HIGH 渠道；
2. 再用该 `channelId` 发通知。

> 已在较低重要性下创建过的渠道无法升级，只能换新的 channelId 或卸载重装。

## 版本

- Kuikly：见 `buildSrc/src/main/java/KotlinBuildVar.kt`
- Android minSdk 21 / iOS 12+ / HarmonyOS NEXT

## License

Apache-2.0
