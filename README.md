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

## 厂商适配与设置引导（Android）

国内 ROM 对后台 / 本地通知有额外限制，**相关资质与参数由宿主开发者自行申请、配置，本库不代为申请**：

| 事项 | 谁负责 | 说明 |
|---|---|---|
| 小米「后台发送本地通知」白名单 | 宿主 | 小米默认禁止后台发本地通知，需向小米申请白名单，或改用小米推送 |
| 华为「消息自分类」权益 | 宿主 | 未申请时本地通知按「资讯营销类」限频 |
| 自启动 / 后台白名单 | 宿主 + 用户 | 影响定时 / 重复通知在应用被杀后能否触发 |
| 厂商推送通道参数 | 宿主（未来 KuiklyPush） | 仅离线推送需要（小米 / 华为 / 荣耀 / OPPO / vivo / 魅族） |
| Android 角标 | 宿主（或二期组件） | 无统一 API，需按厂商单独适配 |

本库提供**设置引导**能力（Android 4 个全实现；鸿蒙 / iOS 仅 `openNotificationSettings` 可用，其余回调 `UNSUPPORTED`）：

```kotlin
module.isBatteryOptimizationEnabled { r -> /* r.data: {"enabled": true} 表示电池优化开启（未加白名单） */ }
module.openBatteryOptimizationSettings { }
module.openAutoStartSettings { }   // 小米/华为/荣耀/OPPO/vivo/魅族；无对应页面时回退应用详情
module.openNotificationSettings { }
```

> ⚠️ 定时 / 重复通知在小米、华为等 OEM 上可能因后台限制不触发，**必须真机验证**。

## 鸿蒙产物编译（Kotlin/Native）

鸿蒙端的 Kuikly 产物需用腾讯定制版 Kotlin 工具链单独编译（官方 Kotlin 不支持 `ohosArm64`）：

```bash
# OHOS_SDK_HOME 指向 DevEco 的 openharmony SDK
export OHOS_SDK_HOME="/path/to/DevEco Studio/sdk/default/openharmony"
./gradlew -c settings.ohos.gradle.kts :shared:linkSharedDebugSharedOhosArm64
# 产物: shared/build/bin/ohosArm64/sharedDebugShared/libshared.so + libshared_api.h
```

> 说明：`settings.ohos.gradle.kts` 是独立的鸿蒙编译链（Kotlin `2.0.21-KBA-010` + Kuikly `2.24.0-2.0.21-ohos`），与 Android/iOS 使用的 Kotlin `2.1.21` 相互隔离。

### 鸿蒙已知限制（实测）

- **代理提醒受管控**：定时 / 重复通知依赖代理提醒，需应用具备 `reminder_capability` 云能力（AGC 侧申请）。未申请时 `publishReminder` 返回 `1700002`（配额 0）。相关资质由**宿主自行申请**，本库不代办。
- **定时提前量不能过短**：实测提前 10 秒 → `401 Parameter error`；30 秒 / 60 秒通过。建议至少留 ≥1 分钟。
- **横幅默认出不来**：鸿蒙要弹顶部横幅，除了渠道用 `SOCIAL_COMMUNICATION` / `SERVICE_INFORMATION`（`LEVEL_HIGH`），还**必须在系统设置里打开「横幅通知」**（该开关默认关闭）。可引导用户到 `设置 → 通知和状态栏 → 本应用 → 提醒方式 → 横幅通知`；实测（API 26 模拟器）开启后横幅立即出现。
- 模拟器限制：Windows / Intel Mac 上的鸿蒙模拟器是 **x86_64**，而 Kuikly 渲染引擎仅提供 **arm64**（截至最新 `@kuikly-open/render` **2.28.0**，包内仍只有 `libs/arm64-v8a/libkuikly.so`；maven `-ohos` 构件也只有 `ohosArm64` 变体），因此 **Kuikly 无法在 Windows / Intel Mac 的鸿蒙模拟器运行**；需鸿蒙真机或 Apple Silicon Mac 的鸿蒙模拟器。

## 版本

- Kuikly：见 `buildSrc/src/main/java/KotlinBuildVar.kt`
- Android minSdk 21 / iOS 12+ / HarmonyOS NEXT

## License

Apache-2.0
