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

## 版本

- Kuikly：见 `buildSrc/src/main/java/KotlinBuildVar.kt`
- Android minSdk 21 / iOS 12+ / HarmonyOS NEXT

## License

Apache-2.0
