# KuiklyNotification

基于 [Kuikly](https://github.com/Tencent-TDS/KuiklyUI) 的**跨端本地通知组件**（Android / iOS / 鸿蒙）。
A cross-platform **local notification** component for [Kuikly](https://github.com/Tencent-TDS/KuiklyUI) (Android / iOS / HarmonyOS).

`v1.0.0` · Apache-2.0 · [中文](#中文) | [English](#english)

---

## 中文

> 本文件为中英双语。**以中文为准**；英文为同步翻译，如有出入请以中文为准。
> 术语对照：通知渠道 = Notification Channel / Slot · 横幅 = Heads-up / Banner · 代理提醒 = Agent-powered Reminder · 冷启动 = Cold start

### 目录

1. [这是什么 / 不是什么](#1-这是什么--不是什么)
2. [能力矩阵](#2-能力矩阵)
3. [快速开始](#3-快速开始)
4. [API 参考](#4-api-参考)
5. [平台差异与已知限制](#5-平台差异与已知限制)
6. [厂商资质与自办清单](#6-厂商资质与自办清单含指引)
7. [宿主接入 Checklist](#7-宿主接入-checklist)
8. [FAQ / 排障](#8-faq--排障)
9. [Demo 与验证](#9-demo-与验证)
10. [版本与兼容性](#10-版本与兼容性)
11. [贡献指南](#11-贡献指南)
12. [License / 支持](#12-license--支持)

---

### 1. 这是什么 / 不是什么

**是**：一个 Kuikly 组件。用一套 Kotlin 代码，在 Android / iOS / 鸿蒙上弹出系统通知栏消息，并支持
权限申请、渠道管理、立即/定时/重复通知、取消、点击回跳、冷启动携带 payload、角标（iOS）。

**不是**：

- ❌ **不代办任何厂商资质**。国内 ROM（小米/华为/荣耀/OPPO/vivo/魅族）与鸿蒙代理提醒的资质、白名单、参数，**由宿主开发者自行申请**。库只提供**设置引导 API**（见 [§6](#6-厂商资质与自办清单含指引)）。
- ❌ **不做远程推送**。本组件只负责**本地通知**（App 自己发起）。离线推送需要厂商推送通道，属于另一个组件。
- ❌ **不做厂商角标**。Android 无统一角标 API，需按厂商单独适配（iOS 角标本组件支持）。
- ❌ **不加密、不改写**通知内容。标题/正文/payload 可能敏感，请自行评估。

---

### 2. 能力矩阵

| 能力 | Android | iOS | 鸿蒙 |
|---|---|---|---|
| 申请 / 查询通知权限 | ✅ | ✅ | ✅ |
| 通知渠道 | ✅ 重要性可调 | ❌ `UNSUPPORTED(1010)` | ⚠️ 等级由 SlotType 固定 |
| 立即发送 / 取消 / 取消全部 | ✅ | ✅ | ✅ |
| 定时通知 | ✅（精确闹钟需额外权限） | ✅（过去时间报错） | ⚠️ 需资质 + 提前量 ≥30 秒 |
| 重复通知 | ✅ | ✅ | ⚠️ 仅 `DAY` / `WEEK` |
| 点击回跳（常驻监听） | ✅ | ✅ | ✅ |
| 冷启动取 payload | ✅ | ✅ | ⚠️ 由宿主注入 want |
| 角标 | ❌ | ✅ | ❌ |
| 设置引导 | ✅ 4 个接口 | ⚠️ 仅通知设置 | ⚠️ 仅通知设置 |

---

### 3. 快速开始

#### 3.1 环境要求

| 项 | 版本 |
|---|---|
| Kuikly | `2.24.0-2.1.21` |
| Kotlin | `2.1.21` |
| AGP / Gradle | `8.4.0` / `8.6` |
| Android `compileSdk` / `minSdk` / `targetSdk` | `34` / `21` / `30` |
| iOS | `12.0+` |
| 鸿蒙 | HarmonyOS NEXT（本仓库实测 API 26） |
| 鸿蒙产物编译链 | Kotlin `2.0.21-KBA-010` + Kuikly `2.24.0-2.0.21-ohos`（见 [§10.2](#102-鸿蒙产物编译独立编译链)） |

> 依赖 Kuikly 运行时。Kuikly 构件托管在腾讯公开只读镜像 `mirrors.tencent.com/nexus/repository/maven-tencent/`，
> 需在宿主 `settings.gradle.kts` 的 `pluginManagement` / `dependencyResolutionManagement` 中加入该仓库。

#### 3.2 引入依赖

本仓库**不上传任何制品仓库**。提供两种引入方式：

**方式 A（推荐）：JitPack** —— 你只需给仓库打 tag，JitPack 会按需现场构建，无需上传。

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") } // Kuikly
    }
}

// 宿主模块 build.gradle.kts
dependencies {
    // 原生实现（AAR）
    implementation("com.github.AriaLEntropy.KuiklyNotification:KuiklyNotificationAndroid:1.0.0")
    // Kuikly 侧公共 API（KMP，取 android 变体）
    implementation("com.github.AriaLEntropy.KuiklyNotification:KuiklyNotification:1.0.0")
}
```

> 首次解析时 JitPack 需要现场构建，耗时较长；若失败，请用方式 B 并在 Issue 反馈。

**方式 B（保底）：源码引入** —— clone 本仓库后二选一：

```bash
git clone https://github.com/AriaLEntropy/KuiklyNotification.git

# B1. composite build（宿主 settings.gradle.kts 加）
includeBuild("../KuiklyNotification")
# B2. 或先发到本地仓库，再在宿主加 mavenLocal()
./gradlew :KuiklyNotification:publishToMavenLocal :KuiklyNotificationAndroid:publishToMavenLocal
```

**iOS（CocoaPods，无需发布 trunk）**

```ruby
# 直接用 git tag（推荐）
pod 'KuiklyNotificationIOS', :git => 'https://github.com/AriaLEntropy/KuiklyNotification.git', :tag => '1.0.0'
pod 'OpenKuiklyIOSRender', '~> 2.24.0'   # Kuikly iOS 渲染库，版本与宿主 Kuikly 一致
```

**鸿蒙（HAR）** —— ohpm 不支持 git 直连，需先编出 HAR：

```bash
# 产物：KuiklyNotificationOhos/build/default/outputs/default/KuiklyNotificationOhos.har
hvigorw assembleHar
```

然后在宿主 `oh-package.json5` 中引用本地 HAR 或源码 module：

```json5
{
  "dependencies": {
    "@arialentropy/kuikly-notification-ohos": "file:../KuiklyNotificationOhos"
  }
}
```

#### 3.3 通用第一步：`configure`

```kotlin
val notification = acquireModule<KRNotificationModule>(KRNotificationModule.MODULE_NAME)

notification.configure(
    NotificationConfig(
        hostActivity = "com.example.app.MainActivity",  // Android：入口 Activity 全限定类名
        smallIconResId = R.drawable.ic_notify,          // Android：通知小图标
        hostBundleName = "com.example.app",             // 鸿蒙：bundleName
        hostAbilityName = "EntryAbility"                // 鸿蒙：入口 AbilityName
    )
) { result -> /* result.code == 0 表示成功 */ }
```

| 参数 | 端 | 必填 | 说明 |
|---|---|---|---|
| `hostActivity` | Android | ✅ | 入口 Activity **全限定类名**；点击通知的回跳目标 |
| `smallIconResId` | Android | ✅ | 通知小图标资源 id |
| `hostBundleName` | 鸿蒙 | ✅ | 应用 bundleName |
| `hostAbilityName` | 鸿蒙 | ✅ | 入口 AbilityName |

> iOS 无需 `configure`（delegate 机制，见 [§5](#5-平台差异与已知限制)）。iOS 调用也安全（空实现）。

#### 3.4 Android 接入

**1）Manifest 声明权限**（API 33+ 通知运行时权限）

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**2）入口 Activity 声明 `singleTask`**（否则 App 存活时点击通知会新建实例）

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask"
    android:exported="true" />
```

**3）在 `onCreate` / `onNewIntent` 上报 Intent**（冷启动 / 点击 payload）

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    KRNotificationLaunch.onNewIntent(this, intent)
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    KRNotificationLaunch.onNewIntent(this, intent)
}
```

**4）注册原生 Module**

```kotlin
override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
    super.registerExternalModule(kuiklyRenderExport)
    with(kuiklyRenderExport) {
        moduleExport(KRNotificationModule.MODULE_NAME) { KRNotificationModule() }
    }
}
```

**5）Kuikly 侧注册**（页面 `createExternalModules()`）

```kotlin
override fun createExternalModules(): Map<String, Module>? =
    hashMapOf(KRNotificationModule.MODULE_NAME to KRNotificationModule())
```

**6）`configure()`** → 见 [§3.3](#33-通用第一步configure)。

> 定时/重复通知到点由组件内置 `BroadcastReceiver` 弹出，无需宿主声明。

#### 3.5 iOS 接入

**1）Podfile** → 见 [§3.2](#32-引入依赖)。**注意**：若宿主已包含 `shared` 这类含 KMP 层的静态 framework，
**不要再单独引 `KuiklyNotification` pod**，否则链接期会产生大量 duplicate symbols（实测约 15585 个）。

**2）无需额外配置**：组件在 `+load` 阶段自动设置 `UNUserNotificationCenter.delegate`，宿主**不要**重复设置。

**3）页面取冷启动 payload**：

```kotlin
override fun created() {
    super.created()
    notification.getLaunchNotification()?.let { event ->
        // event.id / event.payload
    }
    notification.setNotificationClickListener { event -> /* 点击事件 */ }
}
```

#### 3.6 鸿蒙接入

**1）`module.json5` 声明权限**

```json5
"requestPermissions": [
  { "name": "ohos.permission.PUBLISH_AGENT_REMINDER" }   // 定时/重复通知需要
  // 不要声明 NOTIFICATION_CONTROLLER（系统级，普通应用不可用）
]
```

**2）注册 Module**（宿主 Kuikly 视图 delegate）

```ts
getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator> {
  const map: Map<string, KRRenderModuleExportCreator> = new Map();
  map.set(KRNotificationModule.MODULE_NAME, () => new KRNotificationModule(this.uiAbilityContext));
  return map;
}
```

**3）`configure()` 传入 `hostBundleName` + `hostAbilityName`** → 见 [§3.3](#33-通用第一步configure)。

**4）冷启动 / 点击回跳**：入口 Ability 在 `onCreate` / `onNewWant` 调用

```ts
this.notificationModule?.populateLaunchNotification(want.parameters);
```

> ⚠️ Kuikly 鸿蒙**引擎**（`libkuikly.so`）只有 **arm64** 版本，Windows / Intel Mac 的鸿蒙模拟器是 x86_64，
> **跑不了 Kuikly**（见 [§9.3](#93-鸿蒙只能跑纯-arkts-验证-demo)）。

#### 3.7 自检

接入完成后按下表逐项验证（三端一致）：

- [ ] `checkPermission` 返回 `NOT_DETERMINED` / `DENIED`
- [ ] `requestPermission` 弹出系统授权框（Android 13+ / iOS / 鸿蒙）
- [ ] `show()` 后通知出现在通知栏
- [ ] 点击通知 → 监听器收到 `payload`；杀进程后点击 → 冷启动能取到
- [ ] `cancel` / `cancelAll` 生效

---

### 4. API 参考

#### 4.1 方法总表

`Module` 名固定为 `KRNotificationModule`（三端一致）。除特别说明外均为**异步**，通过 `JsonResultCallback` 回包。

| 方法 | 入参 | 回调 `data` | Android | iOS | 鸿蒙 |
|---|---|---|---|---|---|
| `configure(config, cb)` | `NotificationConfig` | `{}` | ✅ | ✅（可省） | ✅ |
| `requestPermission(cb)` | — | `{status}` | ✅ | ✅ | ✅ |
| `checkPermission(cb)` | — | `{status}` | ✅ | ✅ | ✅ |
| `createChannel(channelId, name, importance, cb)` | `String, String, String` | `{}` | ✅ | ❌ 1010 | ✅ |
| `show(request, cb)` | `NotificationRequest` | `{}` | ✅ | ✅ | ✅ |
| `scheduleAt(request, timestampMs, cb)` | `+Long` | `{}` | ✅ | ✅ | ⚠️ |
| `showPeriodically(request, interval, cb)` | `+String` | `{}` | ✅ | ✅ | ⚠️ |
| `cancel(id, cb)` | `Int` | `{}` | ✅ | ✅ | ✅ |
| `cancelAll(cb)` | — | `{}` | ✅ | ✅ | ✅ |
| `setBadge(count, cb)` | `Int` | `{}` | ❌ 1010 | ✅ | ❌ 1010 |
| `getBadge(cb)` | — | `{count}` | ❌ 1010 | ✅ | ❌ 1010 |
| `isBatteryOptimizationEnabled(cb)` | — | `{enabled}` | ✅ | ❌ 1010 | ❌ 1010 |
| `openBatteryOptimizationSettings(cb)` | — | `{}` | ✅ | ❌ 1010 | ❌ 1010 |
| `openAutoStartSettings(cb)` | — | `{}` | ✅ | ❌ 1010 | ❌ 1010 |
| `openNotificationSettings(cb)` | — | `{}` | ✅ | ✅ | ✅（API 13+） |
| `setNotificationClickListener(listener)` | `(NotificationClickEvent) -> Unit` | 点击事件 | ✅ | ✅ | ✅ |
| `removeNotificationClickListener()` | — | — | ✅ | ✅ | ✅ |
| `getLaunchNotification()` | — | **同步**返回 `NotificationClickEvent?` | ✅ | ✅ | ✅ |

#### 4.2 数据模型

**`NotificationConfig`**

| 字段 | 类型 | 说明 |
|---|---|---|
| `hostActivity` | `String?` | Android 入口 Activity 全限定类名 |
| `smallIconResId` | `Int?` | Android 通知小图标资源 id |
| `hostBundleName` | `String?` | 鸿蒙 bundleName |
| `hostAbilityName` | `String?` | 鸿蒙入口 AbilityName |

**`NotificationRequest`**

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `id` | `Int` | — | 唯一标识，**重复 id 会覆盖** |
| `title` / `body` | `String` | — | 标题 / 正文 |
| `channelId` | `String` | `"default"` | 渠道 id（Android / 鸿蒙） |
| `payload` | `String?` | `null` | 点击原样返回，建议 ≤1KB |
| `showWhenInForeground` | `Boolean` | `false` | App 在前台时是否仍展示 |
| `badge` | `Int?` | `null` | iOS 角标 |
| `sound` | `String?` | `null` | Android raw 名 / iOS 带后缀文件名 / 鸿蒙 rawfile 名 |
| `groupKey` | `String?` | `null` | Android `group` / iOS `threadIdentifier` |

**`NotificationClickEvent`**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Int` | 通知 id |
| `payload` | `String?` | 发送时携带的 payload |
| `action` | `String` | 预留，默认 `"default"` |

**常量**

```kotlin
NotificationConst.Importance  // HIGH / DEFAULT / LOW / MIN
NotificationConst.Interval    // MINUTE / HOUR / HALF_DAY / DAY / WEEK
NotificationConst.PermissionStatus // GRANTED / DENIED / NOT_DETERMINED / ERROR
```

#### 4.3 回调格式

所有异步回调统一为：

```json
{ "code": 0, "msg": "", "data": { } }
```

- `code == 0` 成功；其余为下方错误码
- `data` 形状见 [§4.1](#41-方法总表)；多数方法为 `{}`
- `getLaunchNotification()` 是**同步**方法，直接返回 `NotificationClickEvent?`，**不是** `JsonResult`

#### 4.4 错误码

| code | 常量 | 含义 / 常见原因 |
|---|---|---|
| `0` | `SUCCESS` | 成功 |
| `-1` | `FAILED` | Native 回包异常（极少） |
| `1001` | `INVALID_CONFIG` | 未 `configure` 或缺少必需参数（Android 缺 `hostActivity`/`smallIconResId`；鸿蒙缺 bundle/ability） |
| `1002` | `PERMISSION_DENIED` | 无通知权限；`show()` 被拒 |
| `1003` | `PERMISSION_NOT_DETERMINED` | 尚未申请过（`checkPermission`） |
| `1004` | `CHANNEL_NOT_FOUND` | 渠道不存在（Android 回退 default） |
| `1005` | `CHANNEL_DISABLED` | 渠道被用户关闭 |
| `1006` | `PAST_TIME_NOT_ALLOWED` | `scheduleAt` 传了过去时间（iOS 明确报错） |
| `1007` | `UNSUPPORTED_INTERVAL` | 间隔不支持（鸿蒙仅 `DAY`/`WEEK`） |
| `1008` | `BAD_SOUND_FORMAT` | 声音资源格式/名称非法 |
| `1009` | `REMINDER_NOT_ALLOWED` | 代理提醒被管控（鸿蒙需资质；见 [§5](#5-平台差异与已知限制)） |
| `1010` | `UNSUPPORTED` | 该端不支持此能力（如 iOS 调 `createChannel`） |
| `1011` | `INVALID_REQUEST` | 参数非法 / 未知方法 |

> 鸿蒙原生错误码映射：`1700001` 通知未开启、`1700002` 代理提醒配额/资质不足、`401` 参数错误（多为提前量过短）。
> 组件会归一到上表（`401` 目前按 `INVALID_REQUEST` 返回；若需细分可提 Issue）。

---

### 5. 平台差异与已知限制

#### 5.1 三端速查

| 维度 | Android | iOS | 鸿蒙 |
|---|---|---|---|
| 请求权限 | **<13 无运行时权限，直接返回 `GRANTED` 不弹窗**；13+ 首次弹窗；**拒绝后不再弹** | 首次弹系统框；拒绝后不再弹，需去设置 | 首次弹系统框；拒绝后 `requestEnableNotification` 不再弹，需走设置 |
| `checkPermission` | 未申请过返回 `NOT_DETERMINED` | 同 | 同 |
| 渠道 | `NotificationChannel`，`importance` 可调；**创建后重要性不可修改** | 无渠道概念 → `1010` | `addSlot(type)`，**等级由类型固定**：`HIGH→SOCIAL_COMMUNICATION`、`DEFAULT→SERVICE_INFORMATION`、`LOW/MIN→CONTENT_INFORMATION` |
| 横幅 | **仅 `IMPORTANCE_HIGH` 渠道**；已按低重要性建过的渠道无法升级，只能换新 `channelId` | 由系统策略 + `showWhenInForeground` 决定 | 需 `LEVEL_HIGH` 渠道（社交通讯/服务提醒）**且系统「横幅通知」开关打开（默认关闭）** |
| 定时 | `AlarmManager`；精确需 `SCHEDULE_EXACT_ALARM` | `UNCalendarNotificationTrigger`；**过去时间 → `1006`** | 代理提醒；**需 `reminder_capability` 资质**，且**提前量 ≥30 秒**（实测 10s → `401`） |
| 重复 | `setRepeating` | `UNTimeIntervalNotificationTrigger(repeats: true)` | 仅 `DAY`/`WEEK`（Alarm）；`MINUTE/HOUR/HALF_DAY → 1007` |
| 角标 | ❌ | ✅（16+ `setBadgeCount`；≤15 `applicationIconBadgeNumber`） | ❌ |
| 前后台判断 | `ProcessLifecycleOwner` | `willPresent` options | `applicationStateManager` |
| 冷启动 payload | `KRNotificationLaunch.onNewIntent()` 缓存 | delegate `didReceive` 自动缓存 | 宿主 `populateLaunchNotification(want.parameters)` |
| 设置引导 | 4 个接口全支持 | 仅 `openNotificationSettings` | 仅 `openNotificationSettings`（API 13+） |

#### 5.2 明确不支持

- Android 厂商角标（小米/华为/OPPO/vivo 各自接口，无统一 API）
- 远程/离线推送（需厂商推送通道）
- 厂商推送通道参数配置

---

### 6. 厂商资质与自办清单（含指引）

> **本库不代办任何资质**。下列事项由**宿主开发者**在接入时自行申请/配置。
> 各厂商后台改版频繁，**入口与流程以官方最新为准**。

#### 6.1 Android

| 事项 | 谁负责 | 不做的症状 | 指引 |
|---|---|---|---|
| 小米「后台发送本地通知」白名单 | 宿主 | 应用退到后台后发的本地通知**不展示** | 小米开放平台（`dev.mi.com`）申请白名单；或改用小米推送 |
| 华为 / 荣耀「消息自分类」权益 | 宿主 | 本地通知被按「资讯营销类」限频 | 华为 AppGallery Connect（`developer.huawei.com`）→ 应用 → 消息自分类 |
| 自启动 / 后台运行白名单 | 宿主 + 用户 | 定时/重复通知在应用被杀后**不触发** | 库提供 `openAutoStartSettings()` 直接跳转各厂商页面 |
| 精确闹钟 `SCHEDULE_EXACT_ALARM` | 宿主 | `scheduleAt` 时间不准 | Android 官方文档；上架 Google Play 需符合其政策 |
| 电池优化白名单 | 宿主 + 用户 | 后台被限制 | 库提供 `isBatteryOptimizationEnabled()` / `openBatteryOptimizationSettings()` |

```kotlin
// 引导 API（Android 4 个；iOS/鸿蒙仅 openNotificationSettings）
notification.isBatteryOptimizationEnabled { r -> /* r.data: {"enabled": true} = 电池优化开启（未加白名单） */ }
notification.openBatteryOptimizationSettings { }
notification.openAutoStartSettings { }        // 小米/华为/荣耀/OPPO/vivo/魅族；无对应页面时回退应用详情
notification.openNotificationSettings { }
```

#### 6.2 鸿蒙

| 事项 | 谁负责 | 不做的症状 | 指引 |
|---|---|---|---|
| 代理提醒 `reminder_capability` | 宿主 | `publishReminder` 返回 **`1700002`**（配额 0），定时/重复通知不可用 | 华为 AppGallery Connect（`developer.huawei.com`）→ 应用 → 配置「代理提醒」相关能力 |
| 系统「横幅通知」开关 | 用户（宿主引导） | 通知只进通知栏，**不弹横幅** | `设置 → 通知和状态栏 → 本应用 → 提醒方式 → 横幅通知`；库提供 `openNotificationSettings()` |

#### 6.3 其他

- **iOS**：无厂商资质要求；通知权限由系统弹窗授予。
- **Google Play 渠道**：`SCHEDULE_EXACT_ALARM` 属敏感权限，上架前需确认政策合规。

---

### 7. 宿主接入 Checklist

#### Android

- [ ] Manifest 声明 `POST_NOTIFICATIONS`
- [ ] 入口 Activity `launchMode="singleTask"`
- [ ] `onCreate` / `onNewIntent` 调 `KRNotificationLaunch.onNewIntent(this, intent)`
- [ ] `registerExternalModule` 注册 `KRNotificationModule`
- [ ] Kuikly 页面 `createExternalModules()` 注册
- [ ] `configure()` 传 `hostActivity` + `smallIconResId`
- [ ] 需要横幅 → 建 `IMPORTANCE_HIGH` 渠道后再发
- [ ] 国内 ROM：申请/引导厂商白名单与自启动（[§6.1](#61-android)）

#### iOS

- [ ] Podfile 引入（**不要与含 KMP 层的静态 framework 重复引**）
- [ ] 不要重复设置 `UNUserNotificationCenter.delegate`
- [ ] 页面 `created()` 取 `getLaunchNotification()`

#### 鸿蒙

- [ ] 编出 / 引入 HAR
- [ ] `module.json5` 声明 `PUBLISH_AGENT_REMINDER`（不声明 `NOTIFICATION_CONTROLLER`）
- [ ] `getCustomRenderModuleCreatorRegisterMap` 注册 Module
- [ ] `configure()` 传 `hostBundleName` + `hostAbilityName`
- [ ] 入口 Ability 调 `populateLaunchNotification(want.parameters)`
- [ ] 定时/重复通知：申请 `reminder_capability`；提前量 ≥30 秒
- [ ] 横幅：引导用户开系统「横幅通知」
- [ ] **真机验证**（arm64；见 [§9.3](#93-鸿蒙只能跑纯-arkts-验证-demo)）

#### 真机验证清单（三端）

- [ ] 前台 / 后台 / 进程被杀 三种状态下的通知展示
- [ ] 点击回跳 + 冷启动 payload
- [ ] 定时 / 重复通知按预期触发
- [ ] 横幅（Android HIGH 渠道 / 鸿蒙系统开关）
- [ ] 角标（iOS）

---

### 8. FAQ / 排障

**Q：调了 `show()` 但通知栏没有？**
1. `checkPermission()` 是否为 `GRANTED`？
2. Android：8.0+ **必须先建渠道**（`createChannel`）；`channelId` 是否一致？
3. 国内 ROM：是否被厂商管控（小米后台本地通知、华为频次）？→ [§6.1](#61-android)
4. 鸿蒙：只想看横幅？系统「横幅通知」开关默认关闭 → [`openNotificationSettings()`](#62-鸿蒙)
5. App 在前台？`showWhenInForeground` 默认 `false`。

**Q：`scheduleAt` 不触发？**
- Android：精确度受 `SCHEDULE_EXACT_ALARM` 与厂商后台策略影响；引导用户加自启动白名单。
- iOS：过去时间会返回 `1006`。
- 鸿蒙：需 `reminder_capability` 资质，否则 `1009`；提前量 <30 秒会 `401`。

**Q：点击通知收不到 payload？**
- Android：入口 Activity 是否 `singleTask`？`onNewIntent` 是否调了 `KRNotificationLaunch.onNewIntent()`？
- 鸿蒙：入口 Ability 是否调了 `populateLaunchNotification(want.parameters)`？
- iOS：是否覆盖了系统 delegate？（不要设）

**Q：`requestPermission` 不弹窗？**
- Android **12 及以下**：系统没有通知运行时权限，直接返回 `GRANTED`，**这是正常的**。
- 任意端**拒绝过之后**：系统不再弹，需引导用户去设置（[`openNotificationSettings()`](#4-api-参考)）。

**Q：`createChannel` 返回 `1010`？**
- 在 iOS 上调用。iOS 没有渠道概念，属预期。

**Q：鸿蒙模拟器上跑不起来？**
- 见 [§9.3](#93-鸿蒙只能跑纯-arkts-验证-demo)：**Kuikly 引擎仅 arm64**。

---

### 9. Demo 与验证

本仓库提供三端 Demo：

#### 9.1 Android（Windows / macOS / Linux 均可编译）

```bash
./gradlew :androidApp:assembleDebug
# 产物：androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

#### 9.2 iOS（需 macOS + Xcode）

见 [`iosApp/README.md`](iosApp/README.md)：创建宿主工程 → 配置 Podfile → `pod install`。

#### 9.3 鸿蒙：只能跑纯 ArkTS 验证 Demo

Kuikly 的鸿蒙渲染引擎 `libkuikly.so` **只有 arm64 版本**（截至最新 `@kuikly-open/render` **2.28.0**，
包内仍只有 `libs/arm64-v8a/`；maven `-ohos` 构件也只有 `ohosArm64` 变体），而
**Windows / Intel Mac 的鸿蒙模拟器是 x86_64**，且系统无 ARM 指令翻译层 → **Kuikly 无法在该模拟器运行**。

| 设备 | 架构 | 能跑 Kuikly |
|---|---|---|
| Windows / Intel Mac 鸿蒙模拟器 | x86_64 | ❌ |
| Apple Silicon Mac 鸿蒙模拟器 | arm64 | ✅ |
| 鸿蒙真机 | arm64 | ✅ |

因此本仓库额外提供 **`ohosApp/entry`（纯 ArkTS 验证 Demo）**，不含任何原生库，可在 x86 模拟器直接验证
鸿蒙通知 API 行为（权限 / 渠道 / 立即通知 / 横幅 / 点击回跳 / 代理提醒 / 取消）。详见 [`ohosApp/README.md`](ohosApp/README.md)。

```bash
# 在 ohosApp 目录
hvigorw assembleHap --no-daemon
hdc install -r entry/build/default/outputs/default/entry-default-unsigned.hap
```

---

### 10. 版本与兼容性

#### 10.1 版本表

| 项 | 值 |
|---|---|
| 本库 | `1.0.0` |
| Kuikly | `2.24.0-2.1.21` |
| Kotlin | `2.1.21` |
| AGP / Gradle | `8.4.0` / `8.6` |
| Android SDK | `compileSdk 34` / `minSdk 21` / `targetSdk 30` |
| iOS | `12.0+` |
| 鸿蒙 | HarmonyOS NEXT（实测 API 26） |

版本集中配置在 [`buildSrc/src/main/java/KotlinBuildVar.kt`](buildSrc/src/main/java/KotlinBuildVar.kt)（单一来源）。

#### 10.2 鸿蒙产物编译（独立编译链）

鸿蒙产物需用**腾讯定制版 Kotlin 工具链**单独编译（官方 Kotlin 不支持 `ohosArm64`），
且 Kuikly 构件要用 `-ohos` 后缀版本，故使用**独立 settings**：

```bash
# OHOS_SDK_HOME 指向 DevEco 的 openharmony SDK
export OHOS_SDK_HOME="/path/to/DevEco Studio/sdk/default/openharmony"
./gradlew -c settings.ohos.gradle.kts :shared:linkSharedDebugSharedOhosArm64
# 产物：shared/build/bin/ohosArm64/sharedDebugShared/libshared.so + libshared_api.h
```

- `settings.ohos.gradle.kts` / `build.ohos.gradle.kts`：Kotlin `2.0.21-KBA-010` + Kuikly `2.24.0-2.0.21-ohos`
- 与 Android/iOS 使用的 Kotlin `2.1.21` **相互隔离**，互不影响

---

### 11. 贡献指南

#### 11.1 工程结构

```
KuiklyNotification/            Kuikly 侧（KMP）：Module、数据模型、常量、错误码
KuiklyNotificationAndroid/     Android 实现（AAR）
KuiklyNotificationIOS/         iOS 实现（静态库源码 + podspec）
KuiklyNotificationOhos/        鸿蒙实现（ArkTS HAR）
shared/                        跨端 Demo 页面（@Page("router")）
androidApp/ iosApp/ ohosApp/   三端壳工程
buildSrc/                      版本与坐标单一来源
```

#### 11.2 本地构建

```bash
# Android（任意平台可编译）
./gradlew :androidApp:assembleDebug

# 仅编组件
./gradlew :KuiklyNotification:assemble :KuiklyNotificationAndroid:assemble

# iOS（macOS）：先生成 dummy framework 再 pod install
./gradlew :shared:generateDummyFramework
cd iosApp && pod install

# 鸿蒙 HAR
cd ohosApp && hvigorw assembleHar

# 鸿蒙跨端产物（独立编译链，需 OHOS_SDK_HOME）
./gradlew -c settings.ohos.gradle.kts :shared:linkSharedDebugSharedOhosArm64
```

#### 11.3 新增一个能力的约定

1. Kuikly 侧 `KuiklyNotification/src/commonMain` 加方法（`KRNotificationModule` 是具体类，不依赖 expect/actual）
2. 三端各自实现：`KRNotificationModule`(Android) / `.m`(iOS，按方法名反射分发) / `.ets`(鸿蒙，`call()` 分发)
3. **统一回调**：native 回包 `{"code","msg","data"}`，错误码复用 `NotificationConst.ErrorCode`
4. **端不支持的**必须回调 `UNSUPPORTED(1010)`，不要静默失败
5. 平台差异登记到本文档 [§5](#5-平台差异与已知限制)，并同步 `requirements/contract` 设计文档
6. **中英双语同步更新**

#### 11.4 提交约定

- 一个提交一个关注点；能编译就本地验证
- 语义化提交：`feat(android):` / `fix(ohos):` / `docs:` / `style(demo):`
- 改了公共 API：必须同时更新文档两语言

#### 11.5 测试

`KuiklyNotification` 已预留 `commonTest`（Kotlin Test）。当前以**三端 Demo + 真机验证**为主（见 [§9](#9-demo-与验证)）。

---

### 12. License / 支持

- 仓库：<https://github.com/AriaLEntropy/KuiklyNotification>
- 问题反馈：<https://github.com/AriaLEntropy/KuiklyNotification/issues>
- License：Apache-2.0

---

## English

> This file is bilingual. **Chinese is authoritative**; the English text is a synchronized translation.
> Glossary: 通知渠道 = Notification Channel / Slot · 横幅 = Heads-up / Banner · 代理提醒 = Agent-powered Reminder · 冷启动 = Cold start

### Table of contents

1. [What it is / is not](#1-what-it-is--is-not)
2. [Capability matrix](#2-capability-matrix)
3. [Getting started](#3-getting-started)
4. [API reference](#4-api-reference)
5. [Platform differences & limitations](#5-platform-differences--limitations)
6. [Vendor qualifications](#6-vendor-qualifications)
7. [Host integration checklist](#7-host-integration-checklist)
8. [FAQ / Troubleshooting](#8-faq--troubleshooting)
9. [Demos & verification](#9-demos--verification)
10. [Versions & compatibility](#10-versions--compatibility)
11. [Contributing](#11-contributing)
12. [License / Support](#12-license--support)

---

### 1. What it is / is not

**It is** a Kuikly component that shows local notifications in the system tray on Android / iOS / HarmonyOS from one
Kotlin codebase, with permission handling, channels, immediate/scheduled/repeating notifications, cancel,
tap-to-open, cold-start payload and badges (iOS).

**It is not**:

- ❌ **It does not obtain vendor qualifications for you.** Chinese OEMs (Xiaomi/Huawei/Honor/OPPO/vivo/Meizu) and
  HarmonyOS agent-powered reminders require qualifications/whitelists that **the host app must apply for**.
  The library only provides **settings-guidance APIs** (see [§6](#6-vendor-qualifications)).
- ❌ **No remote push.** Local notifications only. Offline push requires vendor push channels (a separate component).
- ❌ **No OEM badges.** Android has no unified badge API (iOS badges are supported).
- ❌ **No encryption/rewriting** of notification content. Treat title/body/payload as potentially sensitive.

---

### 2. Capability matrix

| Capability | Android | iOS | HarmonyOS |
|---|---|---|---|
| Request / check permission | ✅ | ✅ | ✅ |
| Notification channel | ✅ adjustable importance | ❌ `UNSUPPORTED(1010)` | ⚠️ level fixed by SlotType |
| Show / cancel / cancelAll | ✅ | ✅ | ✅ |
| Scheduled notification | ✅ (exact alarm needs permission) | ✅ (past time errors) | ⚠️ needs qualification + lead time ≥30 s |
| Repeating notification | ✅ | ✅ | ⚠️ `DAY` / `WEEK` only |
| Tap-to-open (persistent listener) | ✅ | ✅ | ✅ |
| Cold-start payload | ✅ | ✅ | ⚠️ host must inject the want |
| Badge | ❌ | ✅ | ❌ |
| Settings guidance | ✅ 4 APIs | ⚠️ notification settings only | ⚠️ notification settings only |

---

### 3. Getting started

#### 3.1 Requirements

| Item | Version |
|---|---|
| Kuikly | `2.24.0-2.1.21` |
| Kotlin | `2.1.21` |
| AGP / Gradle | `8.4.0` / `8.6` |
| Android `compileSdk` / `minSdk` / `targetSdk` | `34` / `21` / `30` |
| iOS | `12.0+` |
| HarmonyOS | HarmonyOS NEXT (verified on API 26) |
| HarmonyOS artifact toolchain | Kotlin `2.0.21-KBA-010` + Kuikly `2.24.0-2.0.21-ohos` (see [§10.2](#102-harmonyos-artifact-build-separate-toolchain)) |

> Requires the Kuikly runtime. Kuikly artifacts are hosted on Tencent's public read-only mirror
> `mirrors.tencent.com/nexus/repository/maven-tencent/`; add it to your `settings.gradle.kts`.

#### 3.2 Adding the dependency

This repository **does not publish to any artifact repository**. Two options:

**Option A (recommended): JitPack** — just tag the repo; JitPack builds on demand, nothing to upload.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") } // Kuikly
    }
}

// host module build.gradle.kts
dependencies {
    implementation("com.github.AriaLEntropy.KuiklyNotification:KuiklyNotificationAndroid:1.0.0")
    implementation("com.github.AriaLEntropy.KuiklyNotification:KuiklyNotification:1.0.0")
}
```

> The first resolution triggers an on-demand build on JitPack and may take a while. If it fails, use Option B and
> open an issue.

**Option B (fallback): build from source** — clone the repo, then either:

```bash
git clone https://github.com/AriaLEntropy/KuiklyNotification.git

# B1. composite build (add to the host settings.gradle.kts)
includeBuild("../KuiklyNotification")
# B2. or publish locally first, then add mavenLocal() in the host
./gradlew :KuiklyNotification:publishToMavenLocal :KuiklyNotificationAndroid:publishToMavenLocal
```

**iOS (CocoaPods, no trunk release needed)**

```ruby
pod 'KuiklyNotificationIOS', :git => 'https://github.com/AriaLEntropy/KuiklyNotification.git', :tag => '1.0.0'
pod 'OpenKuiklyIOSRender', '~> 2.24.0'   # match your Kuikly version
```

**HarmonyOS (HAR)** — ohpm cannot reference git directly, so build the HAR first:

```bash
# output: KuiklyNotificationOhos/build/default/outputs/default/KuiklyNotificationOhos.har
hvigorw assembleHar
```

```json5
// host oh-package.json5
{
  "dependencies": {
    "@arialentropy/kuikly-notification-ohos": "file:../KuiklyNotificationOhos"
  }
}
```

#### 3.3 Common step: `configure`

```kotlin
val notification = acquireModule<KRNotificationModule>(KRNotificationModule.MODULE_NAME)

notification.configure(
    NotificationConfig(
        hostActivity = "com.example.app.MainActivity",  // Android: fully-qualified launcher Activity
        smallIconResId = R.drawable.ic_notify,          // Android: small icon
        hostBundleName = "com.example.app",             // HarmonyOS: bundleName
        hostAbilityName = "EntryAbility"                // HarmonyOS: entry ability
    )
) { result -> /* result.code == 0 means success */ }
```

| Parameter | Platform | Required | Notes |
|---|---|---|---|
| `hostActivity` | Android | ✅ | Fully-qualified launcher Activity class name |
| `smallIconResId` | Android | ✅ | Notification small icon resource id |
| `hostBundleName` | HarmonyOS | ✅ | App bundleName |
| `hostAbilityName` | HarmonyOS | ✅ | Entry ability name |

> iOS needs no `configure` (delegate based). Calling it on iOS is harmless (no-op).

#### 3.4 Android

**1) Declare the permission** (API 33+)

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**2) Launch mode `singleTask`**

```xml
<activity android:name=".MainActivity" android:launchMode="singleTask" android:exported="true" />
```

**3) Report the Intent in `onCreate` / `onNewIntent`**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    KRNotificationLaunch.onNewIntent(this, intent)
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    KRNotificationLaunch.onNewIntent(this, intent)
}
```

**4) Register the native module**

```kotlin
override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
    super.registerExternalModule(kuiklyRenderExport)
    with(kuiklyRenderExport) {
        moduleExport(KRNotificationModule.MODULE_NAME) { KRNotificationModule() }
    }
}
```

**5) Register on the Kuikly side** (page `createExternalModules()`)

```kotlin
override fun createExternalModules(): Map<String, Module>? =
    hashMapOf(KRNotificationModule.MODULE_NAME to KRNotificationModule())
```

**6) `configure()`** → see [§3.3](#33-common-step-configure).

> Scheduled/repeating notifications are delivered by an internal `BroadcastReceiver`; no host declaration needed.

#### 3.5 iOS

**1) Podfile** → see [§3.2](#32-adding-the-dependency). **Note**: if the host already links a static framework that
embeds the KMP layer (e.g. `shared`), do **not** add the `KuiklyNotification` pod again — it causes a large number of
duplicate symbols (≈15585 observed).

**2) Nothing else to configure**: the component sets `UNUserNotificationCenter.delegate` in `+load`. Do **not** set it again.

**3) Read the cold-start payload**:

```kotlin
override fun created() {
    super.created()
    notification.getLaunchNotification()?.let { event -> /* event.id / event.payload */ }
    notification.setNotificationClickListener { event -> /* tap event */ }
}
```

#### 3.6 HarmonyOS

**1) Declare the permission in `module.json5`**

```json5
"requestPermissions": [
  { "name": "ohos.permission.PUBLISH_AGENT_REMINDER" }   // needed for scheduled/repeating
  // do NOT declare NOTIFICATION_CONTROLLER (system-level)
]
```

**2) Register the module**

```ts
getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator> {
  const map: Map<string, KRRenderModuleExportCreator> = new Map();
  map.set(KRNotificationModule.MODULE_NAME, () => new KRNotificationModule(this.uiAbilityContext));
  return map;
}
```

**3) `configure()` with `hostBundleName` + `hostAbilityName`** → see [§3.3](#33-common-step-configure).

**4) Cold start / tap**: in the entry ability's `onCreate` / `onNewWant`

```ts
this.notificationModule?.populateLaunchNotification(want.parameters);
```

> ⚠️ The Kuikly HarmonyOS **engine** (`libkuikly.so`) ships **arm64 only**. Windows / Intel Mac HarmonyOS emulators are
> x86_64 and **cannot run Kuikly** (see [§9.3](#93-harmonyos-pure-arkts-verification-demo-only)).

#### 3.7 Self-check

- [ ] `checkPermission` returns `NOT_DETERMINED` / `DENIED`
- [ ] `requestPermission` shows the system dialog (Android 13+ / iOS / HarmonyOS)
- [ ] `show()` puts the notification in the tray
- [ ] Tapping delivers `payload`; after killing the app, cold start still returns it
- [ ] `cancel` / `cancelAll` work

---

### 4. API reference

#### 4.1 Methods

The module name is `KRNotificationModule` on all platforms. All methods are **asynchronous** via
`JsonResultCallback` unless noted.

| Method | Parameters | Callback `data` | Android | iOS | HarmonyOS |
|---|---|---|---|---|---|
| `configure(config, cb)` | `NotificationConfig` | `{}` | ✅ | ✅ (optional) | ✅ |
| `requestPermission(cb)` | — | `{status}` | ✅ | ✅ | ✅ |
| `checkPermission(cb)` | — | `{status}` | ✅ | ✅ | ✅ |
| `createChannel(channelId, name, importance, cb)` | `String, String, String` | `{}` | ✅ | ❌ 1010 | ✅ |
| `show(request, cb)` | `NotificationRequest` | `{}` | ✅ | ✅ | ✅ |
| `scheduleAt(request, timestampMs, cb)` | `+Long` | `{}` | ✅ | ✅ | ⚠️ |
| `showPeriodically(request, interval, cb)` | `+String` | `{}` | ✅ | ✅ | ⚠️ |
| `cancel(id, cb)` | `Int` | `{}` | ✅ | ✅ | ✅ |
| `cancelAll(cb)` | — | `{}` | ✅ | ✅ | ✅ |
| `setBadge(count, cb)` | `Int` | `{}` | ❌ 1010 | ✅ | ❌ 1010 |
| `getBadge(cb)` | — | `{count}` | ❌ 1010 | ✅ | ❌ 1010 |
| `isBatteryOptimizationEnabled(cb)` | — | `{enabled}` | ✅ | ❌ 1010 | ❌ 1010 |
| `openBatteryOptimizationSettings(cb)` | — | `{}` | ✅ | ❌ 1010 | ❌ 1010 |
| `openAutoStartSettings(cb)` | — | `{}` | ✅ | ❌ 1010 | ❌ 1010 |
| `openNotificationSettings(cb)` | — | `{}` | ✅ | ✅ | ✅ (API 13+) |
| `setNotificationClickListener(listener)` | `(NotificationClickEvent) -> Unit` | tap events | ✅ | ✅ | ✅ |
| `removeNotificationClickListener()` | — | — | ✅ | ✅ | ✅ |
| `getLaunchNotification()` | — | **synchronous** `NotificationClickEvent?` | ✅ | ✅ | ✅ |

#### 4.2 Data models

**`NotificationConfig`**

| Field | Type | Notes |
|---|---|---|
| `hostActivity` | `String?` | Android launcher Activity (FQCN) |
| `smallIconResId` | `Int?` | Android small icon resource id |
| `hostBundleName` | `String?` | HarmonyOS bundleName |
| `hostAbilityName` | `String?` | HarmonyOS entry ability name |

**`NotificationRequest`**

| Field | Type | Default | Notes |
|---|---|---|---|
| `id` | `Int` | — | Unique; **the same id overwrites** |
| `title` / `body` | `String` | — | Title / body |
| `channelId` | `String` | `"default"` | Channel id (Android / HarmonyOS) |
| `payload` | `String?` | `null` | Returned verbatim on tap; keep ≤1KB |
| `showWhenInForeground` | `Boolean` | `false` | Show while the app is foreground |
| `badge` | `Int?` | `null` | iOS badge |
| `sound` | `String?` | `null` | Android raw name / iOS filename with extension / HarmonyOS rawfile name |
| `groupKey` | `String?` | `null` | Android `group` / iOS `threadIdentifier` |

**`NotificationClickEvent`**

| Field | Type | Notes |
|---|---|---|
| `id` | `Int` | Notification id |
| `payload` | `String?` | Payload sent with the notification |
| `action` | `String` | Reserved, default `"default"` |

**Constants**

```kotlin
NotificationConst.Importance        // HIGH / DEFAULT / LOW / MIN
NotificationConst.Interval          // MINUTE / HOUR / HALF_DAY / DAY / WEEK
NotificationConst.PermissionStatus  // GRANTED / DENIED / NOT_DETERMINED / ERROR
```

#### 4.3 Callback format

```json
{ "code": 0, "msg": "", "data": { } }
```

- `code == 0` means success; otherwise see the error codes below
- `data` shapes are listed in [§4.1](#41-methods); most methods return `{}`
- `getLaunchNotification()` is **synchronous** and returns `NotificationClickEvent?` — not a `JsonResult`

#### 4.4 Error codes

| code | Constant | Meaning / common cause |
|---|---|---|
| `0` | `SUCCESS` | Success |
| `-1` | `FAILED` | Unexpected native reply (rare) |
| `1001` | `INVALID_CONFIG` | `configure` missing / required params absent (Android `hostActivity`/`smallIconResId`; HarmonyOS bundle/ability) |
| `1002` | `PERMISSION_DENIED` | No notification permission; `show()` rejected |
| `1003` | `PERMISSION_NOT_DETERMINED` | Never requested yet (`checkPermission`) |
| `1004` | `CHANNEL_NOT_FOUND` | Channel missing (Android falls back to default) |
| `1005` | `CHANNEL_DISABLED` | Channel disabled by the user |
| `1006` | `PAST_TIME_NOT_ALLOWED` | `scheduleAt` in the past (explicit error on iOS) |
| `1007` | `UNSUPPORTED_INTERVAL` | Unsupported interval (HarmonyOS: `DAY`/`WEEK` only) |
| `1008` | `BAD_SOUND_FORMAT` | Invalid sound resource/name |
| `1009` | `REMINDER_NOT_ALLOWED` | Agent reminder restricted (HarmonyOS needs qualification) |
| `1010` | `UNSUPPORTED` | Not supported on this platform (e.g. `createChannel` on iOS) |
| `1011` | `INVALID_REQUEST` | Invalid parameters / unknown method |

> HarmonyOS native codes: `1700001` notifications off, `1700002` reminder quota/qualification, `401` invalid parameter
> (usually lead time too short). They are normalized into the table above.

---

### 5. Platform differences & limitations

#### 5.1 Quick comparison

| Aspect | Android | iOS | HarmonyOS |
|---|---|---|---|
| Request permission | **<13: no runtime permission, returns `GRANTED` without dialog**; 13+: dialog on first call; **never again after denial** | Dialog on first call; after denial go to Settings | Dialog on first call; after denial `requestEnableNotification` no longer shows; use Settings |
| Channels | `NotificationChannel`, adjustable importance; **importance is immutable after creation** | no channels → `1010` | `addSlot(type)`, **level fixed by type**: `HIGH→SOCIAL_COMMUNICATION`, `DEFAULT→SERVICE_INFORMATION`, `LOW/MIN→CONTENT_INFORMATION` |
| Heads-up | **`IMPORTANCE_HIGH` channels only**; an existing low-importance channel cannot be upgraded — use a new `channelId` | system policy + `showWhenInForeground` | needs `LEVEL_HIGH` channel **and** the system "banner" toggle (off by default) |
| Scheduled | `AlarmManager`; exact needs `SCHEDULE_EXACT_ALARM` | `UNCalendarNotificationTrigger`; **past time → `1006`** | agent reminder; **needs `reminder_capability`** and **lead time ≥30 s** (10 s → `401`) |
| Repeating | `setRepeating` | `UNTimeIntervalNotificationTrigger(repeats: true)` | `DAY`/`WEEK` only; `MINUTE/HOUR/HALF_DAY → 1007` |
| Badge | ❌ | ✅ (16+ `setBadgeCount`; ≤15 `applicationIconBadgeNumber`) | ❌ |
| Foreground detection | `ProcessLifecycleOwner` | `willPresent` options | `applicationStateManager` |
| Cold-start payload | `KRNotificationLaunch.onNewIntent()` | cached by the delegate | host calls `populateLaunchNotification(want.parameters)` |
| Settings guidance | all 4 APIs | `openNotificationSettings` only | `openNotificationSettings` only (API 13+) |

#### 5.2 Explicitly not supported

- Android OEM badges (no unified API)
- Remote/offline push (requires vendor push channels)
- Vendor push channel configuration

---

### 6. Vendor qualifications

> **The library does not obtain qualifications for you.** The host app must apply/configure the items below.
> Vendor portals change frequently — **refer to the official, latest documentation**.

#### 6.1 Android apps

| Item | Owner | Symptom if missing | Where to look |
|---|---|---|---|
| Xiaomi "post local notifications in background" whitelist | Host | local notifications sent in background are **not shown** | Xiaomi Open Platform (`dev.mi.com`); or switch to Xiaomi Push |
| Huawei / Honor "message self-classification" entitlement | Host | notifications throttled as marketing | Huawei AppGallery Connect (`developer.huawei.com`) → app → message classification |
| Auto-start / background whitelist | Host + user | scheduled/repeating notifications **do not fire** after the app is killed | the library exposes `openAutoStartSettings()` |
| Exact alarm `SCHEDULE_EXACT_ALARM` | Host | `scheduleAt` inaccurate | Android docs; check Google Play policy |
| Battery optimization whitelist | Host + user | background restricted | `isBatteryOptimizationEnabled()` / `openBatteryOptimizationSettings()` |

```kotlin
notification.isBatteryOptimizationEnabled { r -> /* r.data: {"enabled": true} = optimization on (not whitelisted) */ }
notification.openBatteryOptimizationSettings { }
notification.openAutoStartSettings { }
notification.openNotificationSettings { }
```

#### 6.2 HarmonyOS

| Item | Owner | Symptom if missing | Where to look |
|---|---|---|---|
| Agent reminder `reminder_capability` | Host | `publishReminder` returns **`1700002`** (quota 0); scheduled/repeating unavailable | Huawei AppGallery Connect (`developer.huawei.com`) → app → agent reminder capability |
| System "banner notification" toggle | User (guide them) | only in the tray, **no heads-up** | `Settings → Notifications → this app → alert style → banner`; the library exposes `openNotificationSettings()` |

#### 6.3 Others

- **iOS**: no vendor qualification; the system prompts for permission.
- **Google Play**: `SCHEDULE_EXACT_ALARM` is a sensitive permission — verify policy compliance before release.

---

### 7. Host integration checklist

#### Android

- [ ] `POST_NOTIFICATIONS` in the manifest
- [ ] Launcher Activity `launchMode="singleTask"`
- [ ] `KRNotificationLaunch.onNewIntent(this, intent)` in `onCreate` / `onNewIntent`
- [ ] `registerExternalModule` registers `KRNotificationModule`
- [ ] Kuikly page `createExternalModules()`
- [ ] `configure()` with `hostActivity` + `smallIconResId`
- [ ] For heads-up: create an `IMPORTANCE_HIGH` channel first
- [ ] Chinese OEMs: apply for / guide users through vendor whitelists ([§6.1](#61-android-apps))

#### iOS

- [ ] Add the pod (**do not** duplicate it with a static framework that already embeds the KMP layer)
- [ ] Do **not** set `UNUserNotificationCenter.delegate`
- [ ] Call `getLaunchNotification()` in the page's `created()`

#### HarmonyOS

- [ ] Build / reference the HAR
- [ ] Declare `PUBLISH_AGENT_REMINDER` (never `NOTIFICATION_CONTROLLER`)
- [ ] Register the module in `getCustomRenderModuleCreatorRegisterMap`
- [ ] `configure()` with `hostBundleName` + `hostAbilityName`
- [ ] Call `populateLaunchNotification(want.parameters)` in the entry ability
- [ ] Scheduled/repeating: obtain `reminder_capability`; lead time ≥30 s
- [ ] Heads-up: guide the user to enable the system "banner" toggle
- [ ] **Verify on a real device** (arm64; see [§9.3](#93-harmonyos-pure-arkts-verification-demo-only))

#### Real-device checklist (all platforms)

- [ ] Foreground / background / app-killed delivery
- [ ] Tap-to-open + cold-start payload
- [ ] Scheduled / repeating fire as expected
- [ ] Heads-up (Android HIGH channel / HarmonyOS system toggle)
- [ ] Badge (iOS)

---

### 8. FAQ / Troubleshooting

**Q: `show()` runs but nothing appears.**
1. Is `checkPermission()` `GRANTED`?
2. Android 8.0+: **you must create the channel first** (`createChannel`); is `channelId` correct?
3. Chinese OEMs: vendor controls may block background local notifications → [§6.1](#61-android-apps)
4. HarmonyOS heads-up: the system "banner" toggle is off by default → `openNotificationSettings()`
5. Is the app foreground? `showWhenInForeground` defaults to `false`.

**Q: `scheduleAt` never fires.**
- Android: exactness depends on `SCHEDULE_EXACT_ALARM` and OEM background policy; guide users to the auto-start whitelist.
- iOS: past timestamps return `1006`.
- HarmonyOS: needs `reminder_capability` (otherwise `1009`); lead times below 30 s return `401`.

**Q: Tap does not deliver the payload.**
- Android: is the launcher Activity `singleTask`? Is `KRNotificationLaunch.onNewIntent()` called from `onNewIntent`?
- HarmonyOS: did the entry ability call `populateLaunchNotification(want.parameters)`?
- iOS: did you override the system delegate? (don't)

**Q: `requestPermission` shows no dialog.**
- Android **12 and below**: no runtime permission exists; it returns `GRANTED` directly — **expected**.
- Any platform **after a denial**: the system will not prompt again; guide the user to Settings via `openNotificationSettings()`.

**Q: `createChannel` returns `1010`.**
- You called it on iOS. iOS has no channels — expected.

**Q: It fails to run on a HarmonyOS emulator.**
- See [§9.3](#93-harmonyos-pure-arkts-verification-demo-only): **the Kuikly engine is arm64 only**.

---

### 9. Demos & verification

#### 9.1 Android (builds on Windows / macOS / Linux)

```bash
./gradlew :androidApp:assembleDebug
# output: androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

#### 9.2 iOS (macOS + Xcode required)

See [`iosApp/README.md`](iosApp/README.md): create the host project → configure the Podfile → `pod install`.

#### 9.3 HarmonyOS: pure-ArkTS verification demo only

The Kuikly HarmonyOS render engine `libkuikly.so` is **arm64 only** (as of the latest `@kuikly-open/render` **2.28.0**
the package still ships only `libs/arm64-v8a/`; the maven `-ohos` artifacts only expose the `ohosArm64` variant).
Windows / Intel Mac HarmonyOS emulators are **x86_64** with no ARM translation layer → **Kuikly cannot run there**.

| Device | ABI | Runs Kuikly |
|---|---|---|
| Windows / Intel Mac HarmonyOS emulator | x86_64 | ❌ |
| Apple Silicon Mac HarmonyOS emulator | arm64 | ✅ |
| HarmonyOS phone | arm64 | ✅ |

So this repo also ships **`ohosApp/entry` (a pure-ArkTS verification demo)** with no native libraries, runnable on an
x86_64 emulator to verify HarmonyOS notification APIs (permission / channel / immediate / heads-up / tap / agent
reminder / cancel). See [`ohosApp/README.md`](ohosApp/README.md).

```bash
# from the ohosApp directory
hvigorw assembleHap --no-daemon
hdc install -r entry/build/default/outputs/default/entry-default-unsigned.hap
```

---

### 10. Versions & compatibility

#### 10.1 Version table

| Item | Value |
|---|---|
| This library | `1.0.0` |
| Kuikly | `2.24.0-2.1.21` |
| Kotlin | `2.1.21` |
| AGP / Gradle | `8.4.0` / `8.6` |
| Android SDK | `compileSdk 34` / `minSdk 21` / `targetSdk 30` |
| iOS | `12.0+` |
| HarmonyOS | HarmonyOS NEXT (verified API 26) |

Versions live in [`buildSrc/src/main/java/KotlinBuildVar.kt`](buildSrc/src/main/java/KotlinBuildVar.kt) (single source of truth).

#### 10.2 HarmonyOS artifact build (separate toolchain)

HarmonyOS artifacts require Tencent's custom Kotlin toolchain (upstream Kotlin has no `ohosArm64`), and Kuikly
artifacts need the `-ohos` suffix — hence a **separate settings file**:

```bash
export OHOS_SDK_HOME="/path/to/DevEco Studio/sdk/default/openharmony"
./gradlew -c settings.ohos.gradle.kts :shared:linkSharedDebugSharedOhosArm64
# output: shared/build/bin/ohosArm64/sharedDebugShared/libshared.so + libshared_api.h
```

- `settings.ohos.gradle.kts` / `build.ohos.gradle.kts`: Kotlin `2.0.21-KBA-010` + Kuikly `2.24.0-2.0.21-ohos`
- **Isolated** from the Kotlin `2.1.21` used by Android/iOS

---

### 11. Contributing

#### 11.1 Project layout

```
KuiklyNotification/            Kuikly side (KMP): module, models, constants, error codes
KuiklyNotificationAndroid/     Android implementation (AAR)
KuiklyNotificationIOS/         iOS implementation (sources + podspec)
KuiklyNotificationOhos/        HarmonyOS implementation (ArkTS HAR)
shared/                        Cross-platform demo page (@Page("router"))
androidApp/ iosApp/ ohosApp/   Host shells
buildSrc/                      Single source of truth for versions/coordinates
```

#### 11.2 Build locally

```bash
./gradlew :androidApp:assembleDebug                       # Android (any OS)
./gradlew :KuiklyNotification:assemble :KuiklyNotificationAndroid:assemble

./gradlew :shared:generateDummyFramework                   # iOS (macOS)
cd iosApp && pod install

cd ohosApp && hvigorw assembleHar                          # HarmonyOS HAR

./gradlew -c settings.ohos.gradle.kts :shared:linkSharedDebugSharedOhosArm64   # HarmonyOS KN artifact
```

#### 11.3 Adding a capability

1. Add the method on the Kuikly side (`KuiklyNotification/src/commonMain`, `KRNotificationModule` is a concrete class)
2. Implement on all three platforms (`KRNotificationModule`(Android) / `.m`(iOS, selector dispatch) / `.ets`(HarmonyOS, `call()`)
3. **Uniform reply**: `{"code","msg","data"}`, reuse `NotificationConst.ErrorCode`
4. Unsupported platforms **must** reply `UNSUPPORTED(1010)` — never fail silently
5. Register platform differences in [§5](#5-platform-differences--limitations) and keep the design docs in sync
6. **Update both languages in this README**

#### 11.4 Commit conventions

- One concern per commit; verify locally whenever it compiles
- Conventional commits: `feat(android):` / `fix(ohos):` / `docs:` / `style(demo):`
- Public API changes require updating both languages of this README

#### 11.5 Tests

`KuiklyNotification` reserves `commonTest` (Kotlin Test). Verification is currently driven by the three demos plus
real-device testing (see [§9](#9-demos--verification)).

---

### 12. License / Support

- Repository: <https://github.com/AriaLEntropy/KuiklyNotification>
- Issues: <https://github.com/AriaLEntropy/KuiklyNotification/issues>
- License: Apache-2.0
