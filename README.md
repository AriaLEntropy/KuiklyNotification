# KuiklyNotification

[English](README.en.md) | **中文**

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Release](https://img.shields.io/badge/release-v1.0.0-orange.svg)](CHANGELOG.md)
[![Kuikly](https://img.shields.io/badge/Kuikly-2.24.0--2.1.21-4C8BF5.svg)](https://github.com/Tencent-TDS/KuiklyUI)
[![Android](https://img.shields.io/badge/Android-minSdk%2021-3DDC84.svg)](#平台要求)
[![iOS](https://img.shields.io/badge/iOS-12%2B-lightgrey.svg)](#平台要求)
[![HarmonyOS](https://img.shields.io/badge/HarmonyOS-NEXT-black.svg)](#平台要求)

基于 [Kuikly](https://github.com/Tencent-TDS/KuiklyUI) 的**跨端本地通知组件**（Android / iOS / 鸿蒙）。
一套 Kotlin 代码，在系统通知栏弹出消息，支持权限、渠道、立即/定时/重复通知、取消、点击回跳、冷启动 payload、角标（iOS）。

## 目录

1. [这是什么 / 不是什么](#1-这是什么--不是什么)
2. [能力矩阵](#2-能力矩阵)
3. [快速开始](#3-快速开始)
4. [API 参考](#4-api-参考)
5. [平台差异与已知限制](#5-平台差异与已知限制)
6. [厂商资质与自办清单](#6-厂商资质与自办清单含指引)
7. [宿主接入 Checklist](#7-宿主接入-checklist)
8. [FAQ / 排障](#8-faq--排障)
9. [Demo 与验证](#9-demo-与验证)
10. [版本、兼容性与发布](#10-版本兼容性与发布)
11. [隐私与本地数据](#11-隐私与本地数据)
12. [贡献指南](#12-贡献指南)
13. [License / 支持](#13-license--支持)

---

### 1. 这是什么 / 不是什么

**是**：一个 Kuikly 组件，用于展示**本地通知**（App 自己发起），并处理权限、渠道、点击回跳与冷启动。

**不是**：

- ❌ **不代办任何厂商资质**。国内 ROM（小米/华为/荣耀/OPPO/vivo/魅族）与鸿蒙代理提醒的资质、白名单、参数由**宿主自行申请**；库只提供**设置引导 API**（见 [§6](#6-厂商资质与自办清单含指引)）。
- ❌ **不做远程推送**。离线推送需要厂商推送通道，属于另一个组件。
- ❌ **不做厂商角标**。Android 无统一角标 API（iOS 角标本组件支持）。
- ❌ **不加密、不改写**通知内容。标题/正文/payload 可能敏感，请自行评估（另见 [§11 隐私与本地数据](#11-隐私与本地数据)）。

---

### 2. 能力矩阵

| 能力 | Android | iOS | 鸿蒙 |
|---|---|---|---|
| 申请 / 查询通知权限 | ✅ | ✅ | ✅ |
| 通知渠道 | ✅ 重要性可调 | ❌ `UNSUPPORTED(1010)` | ⚠️ 等级由 SlotType 固定 |
| 立即发送 / 取消 / 取消全部 | ✅ | ✅ | ✅ |
| 定时通知 | ✅（精确闹钟需宿主声明权限） | ✅（过去时间报错） | ⚠️ 需资质 + 提前量 ≥30 秒 |
| 重复通知 | ✅ | ✅ | ⚠️ 仅 `DAY` / `WEEK` |
| 点击回跳（常驻监听） | ✅ | ✅ | ✅ |
| 冷启动取 payload | ✅ | ✅ | ⚠️ 由宿主注入 want |
| 角标 | ❌ | ✅ | ❌ |
| 设置引导 | ✅ 4 个接口 | ⚠️ 仅通知设置 | ⚠️ 仅通知设置（API 13+） |

---

### 3. 快速开始

#### 3.1 环境要求

**宿主工程要求**

| 项 | 要求 | 说明 |
|---|---|---|
| Kuikly | `2.24.0-2.1.21`（与宿主一致） | 构件来自腾讯公开只读镜像 |
| Kotlin | `2.1.21` | KMP 模块编译版本 |
| Android | `minSdk 21`、`compileSdk ≥ 34` | **`targetSdk` 由宿主决定**，见下方提示 |
| iOS | `12.0+` | — |
| 鸿蒙 | HarmonyOS NEXT | 本仓库在 API 26 上验证 |

**Android 两个容易踩的前提**

1. **`targetSdk ≥ 33` 才由 `requestPermission` 主动弹授权框**。Android 13+ 上：
   - 宿主 `targetSdk ≥ 33`：调用 `requestPermission` 弹系统框；
   - 宿主 `targetSdk ≤ 32`：系统会在**首次创建通知渠道时**自行弹框，`requestPermission` 的行为由系统决定。
   （本仓库 demo 的 `targetSdk` 是 30，仅用于演示，不代表库要求。）
2. **精确闹钟权限由宿主自行声明**——库的 Manifest 不包含它：
   ```xml
   <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
   ```
   不声明也能用（走非精确闹钟），但触发时间可能不准。

> 本仓库自身的构建版本（Kuikly / Kotlin / AGP / Gradle）见 [§10.1](#101-版本表)。

#### 3.2 引入依赖

**方式 A：Maven 仓库（推荐）**

发布物托管在本仓库的 `gh-pages` 分支（标准 Maven 目录结构），两种地址二选一：

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        // ① 直接走 raw（无需任何设置）
        maven { url = uri("https://raw.githubusercontent.com/AriaLEntropy/KuiklyNotification/gh-pages/") }
        // ② 或在 Settings → Pages 选择 gh-pages 分支后：
        // maven { url = uri("https://arialentropy.github.io/KuiklyNotification/") }
        // Kuikly 运行时
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
    }
}

// 宿主模块 build.gradle.kts
dependencies {
    implementation("io.github.arialentropy:KuiklyNotification:1.0.0")         // Kuikly 侧公共 API
    implementation("io.github.arialentropy:KuiklyNotificationAndroid:1.0.0")  // Android 原生实现
}
```

**方式 B：源码引入（任何情况都可用）**

```bash
git clone https://github.com/AriaLEntropy/KuiklyNotification.git

# B1. composite build：宿主 settings.gradle.kts 加
includeBuild("../KuiklyNotification")
#     然后依赖坐标同上（Gradle 会替换为本地工程）

# B2. 或先发布到本地 Maven 仓库，再在宿主加 mavenLocal()
./gradlew :KuiklyNotification:publishToMavenLocal :KuiklyNotificationAndroid:publishToMavenLocal
```

**iOS（CocoaPods）**

```ruby
# 直接用 git tag（推荐）
pod 'KuiklyNotificationIOS', :git => 'https://github.com/AriaLEntropy/KuiklyNotification.git', :tag => '1.0.0'

# 或本地路径
pod 'KuiklyNotificationIOS', :path => '../KuiklyNotification'

pod 'OpenKuiklyIOSRender', '~> 2.24.0'   # Kuikly iOS 渲染库，版本与宿主 Kuikly 一致
```

> ⚠️ 两个同名 podspec 别引错（见 [§12.2](#122-ios-两个-podspec)）：
> - `KuiklyNotificationIOS`（仓库根）→ **原生实现**，宿主统一引这个；
> - `KuiklyNotification`（`KuiklyNotification/` 目录）→ **KMP 公共 API**，仅在"独立宿主、不含 `shared` 这类已内嵌 KMP 层的静态 framework"时才需要。

**鸿蒙（HAR）**

ohpm 不支持 git 直连，需先编出 HAR：

```bash
cd ohosApp                       # ← 必须在 ohosApp 目录下执行
hvigorw assembleHar
# 产物：KuiklyNotificationOhos/build/default/outputs/default/KuiklyNotificationOhos.har
```

然后在宿主 `oh-package.json5` 引用本地 HAR 或源码 module：

```json5
{
  "dependencies": {
    "@xiaoaiechan/kuikly-notification-ohos": "file:../KuiklyNotificationOhos"
  }
}
```

> 未来若发布到 ohpm 公共仓，即可改为 `ohpm install @xiaoaiechan/kuikly-notification-ohos`。

#### 3.3 通用第一步：`configure`

`NotificationConfig` 由**原生侧**在打开 Kuikly 页面时通过 `pageData` 注入，Kuikly 页面读取后再 `configure`：

```kotlin
// ① 原生侧注入（Android 示例：KuiklyRenderActivity.createPageData()）
pageData["hostActivity"] = KuiklyRenderActivity::class.java.name
pageData["smallIconResId"] = android.R.drawable.ic_dialog_info   // 或宿主自己的 R.drawable.xxx

// ② Kuikly 页面（commonMain）读取并注入给组件
import com.tencent.kuikly.core.pager.Pager
import io.github.arialentropy.notification.model.NotificationConfig
import io.github.arialentropy.notification.module.KRNotificationModule

val notification = acquireModule<KRNotificationModule>(KRNotificationModule.MODULE_NAME)
val params = pageData.params
notification.configure(
    NotificationConfig(
        hostActivity = params.optString("hostActivity").takeIf { it.isNotEmpty() },
        smallIconResId = params.optInt("smallIconResId").takeIf { it != 0 },
        hostBundleName = params.optString("hostBundleName").takeIf { it.isNotEmpty() },  // 鸿蒙
        hostAbilityName = params.optString("hostAbilityName").takeIf { it.isNotEmpty() } // 鸿蒙
    )
) { result -> /* result.code == 0 表示成功 */ }
```

> ⚠️ **不要在 Kuikly 页面里写 `R.drawable.xxx`**：`R` 是 Android 专有符号，commonMain（跨端公共代码）访问不到，会编译失败。必须由原生侧经 `pageData` 传入（如上面 ①）。

| 参数 | 端 | 必填 | 说明 |
|---|---|---|---|
| `hostActivity` | Android | ✅ | 入口 Activity **全限定类名**（点击通知的回跳目标） |
| `smallIconResId` | Android | ✅ | 通知小图标资源 id |
| `hostBundleName` | 鸿蒙 | ✅ | 应用 bundleName |
| `hostAbilityName` | 鸿蒙 | ✅ | 入口 AbilityName |

> iOS 无需 `configure`（delegate 机制）。在 iOS 上调用是安全空实现。

#### 3.4 最小可运行示例

```kotlin
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.module.Module
import io.github.arialentropy.notification.demo.BasePager          // 或你自己的 Pager
import io.github.arialentropy.notification.model.NotificationRequest
import io.github.arialentropy.notification.module.KRNotificationModule

@Page("router")
internal class MyPage : BasePager() {

    private val notification: KRNotificationModule
        get() = acquireModule<KRNotificationModule>(KRNotificationModule.MODULE_NAME)

    // ① 注册 Module（Pager 级别）
    override fun createExternalModules(): Map<String, Module>? =
        hashMapOf(KRNotificationModule.MODULE_NAME to KRNotificationModule())

    override fun created() {
        super.created()

        // ② 权限
        notification.requestPermission { result -> /* data.status = GRANTED / DENIED */ }

        // ③ 渠道（Android 想要横幅必须用 HIGH 渠道）
        notification.createChannel("high", "高优先级", "HIGH") { }

        // ④ 立即发送
        notification.show(
            NotificationRequest(id = 1, title = "标题", body = "内容", channelId = "high", payload = "p1")
        ) { result -> /* result.code == 0 成功 */ }

        // ⑤ 点击监听 + 冷启动
        notification.setNotificationClickListener { event ->
            // event.id / event.payload
        }
        notification.getLaunchNotification()?.let { event ->
            // 冷启动拉起
        }
    }

    override fun body(): ViewBuilder = { /* 页面内容 */ }
}
```

#### 3.5 Android 接入

**1）Manifest 声明权限**

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<!-- 可选：精确闹钟（见 §3.1） -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

**2）入口 Activity 声明 `singleTask`**（否则 App 存活时点击通知会新建实例）

```xml
<activity android:name=".MainActivity" android:launchMode="singleTask" android:exported="true" />
```

**3）在 `onCreate` / `onNewIntent` 上报 Intent**

```kotlin
import io.github.arialentropy.notification.android.KRNotificationLaunch

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
// 注意区分两个同名类（包名不同）：
//   io.github.arialentropy.notification.android.KRNotificationModule   ← 这里是原生实现
//   io.github.arialentropy.notification.module.KRNotificationModule    ← Kuikly 侧公共 API（页面里用）
import io.github.arialentropy.notification.android.KRNotificationModule
import com.tencent.kuikly.core.render.android.IKuiklyRenderExport

override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
    super.registerExternalModule(kuiklyRenderExport)
    with(kuiklyRenderExport) {
        moduleExport(KRNotificationModule.MODULE_NAME) { KRNotificationModule() }
    }
}
```

> 定时/重复通知到点由组件内置 `BroadcastReceiver` 弹出，宿主无需声明。

#### 3.6 iOS 接入

1. Podfile → 见 [§3.2](#32-引入依赖)（注意别重复引，见 [§12.2](#122-ios-两个-podspec)）。
2. **不要重复设置** `UNUserNotificationCenter.delegate`：组件在 `+load` 阶段自动设置。
3. 页面 `created()` 里取 `getLaunchNotification()` 与注册 `setNotificationClickListener`。

#### 3.7 鸿蒙接入

**1）`module.json5` 声明权限**

```json5
"requestPermissions": [
  { "name": "ohos.permission.PUBLISH_AGENT_REMINDER" }   // 定时/重复通知需要
  // 不要声明 NOTIFICATION_CONTROLLER（系统级，普通应用不可用）
]
```

**2）注册 Module**

```ts
getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator> {
  const map: Map<string, KRRenderModuleExportCreator> = new Map();
  map.set(KRNotificationModule.MODULE_NAME, () => new KRNotificationModule(this.uiAbilityContext));
  return map;
}
```

**3）`configure()` 传 `hostBundleName` + `hostAbilityName`** → 见 [§3.3](#33-通用第一步configure)。

**4）冷启动 / 点击回跳**：入口 Ability 的 `onCreate` / `onNewWant`

```ts
this.notificationModule?.populateLaunchNotification(want.parameters);
```

> ⚠️ Kuikly 鸿蒙**引擎**（`libkuikly.so`）只有 **arm64** 版本；Windows / Intel Mac 的鸿蒙模拟器是 x86_64，**跑不了 Kuikly**（见 [§9.3](#93-鸿蒙只能跑纯-arkts-验证-demo)）。

#### 3.8 自检

- [ ] `checkPermission` 返回 `NOT_DETERMINED` / `DENIED`
- [ ] `requestPermission` 弹出系统授权框（Android 13+ / iOS / 鸿蒙）
- [ ] `show()` 后通知出现在通知栏
- [ ] 点击通知 → 监听器收到 `payload`；杀进程后点击 → 冷启动能取到
- [ ] `cancel` / `cancelAll` 生效

---

### 4. API 参考

#### 4.1 方法总表

Module 名固定为 `KRNotificationModule`（三端一致）。除特别说明外均为**异步**，通过 `JsonResultCallback` 回包。

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

**`NotificationConfig`**：`hostActivity`(Android) / `smallIconResId`(Android) / `hostBundleName`(鸿蒙) / `hostAbilityName`(鸿蒙)

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

**`NotificationClickEvent`**：`id: Int` / `payload: String?` / `action: String`（预留，默认 `"default"`）

**常量**

```kotlin
NotificationConst.Importance        // HIGH / DEFAULT / LOW / MIN
NotificationConst.Interval          // MINUTE / HOUR / HALF_DAY / DAY / WEEK
NotificationConst.PermissionStatus  // GRANTED / DENIED / NOT_DETERMINED / ERROR
```

#### 4.3 回调格式

```json
{ "code": 0, "msg": "", "data": { } }
```

- `code == 0` 成功，其余见 [§4.4](#44-错误码)
- `data` 形状见 [§4.1](#41-方法总表)；多数方法为 `{}`
- **`checkPermission` 的未授权状态在 `data.status` 里**（`GRANTED` / `DENIED` / `NOT_DETERMINED`），**不是**通过 `code` 返回
- `getLaunchNotification()` 是**同步**方法，直接返回 `NotificationClickEvent?`，不是 `JsonResult`

#### 4.4 错误码

| code | 常量 | 状态 | 含义 / 常见原因 |
|---|---|---|---|
| `0` | `SUCCESS` | 使用中 | 成功 |
| `-1` | `FAILED` | 使用中 | Native 回包异常（极少） |
| `1001` | `INVALID_CONFIG` | 使用中 | 未 `configure` 或缺少必需参数 |
| `1002` | `PERMISSION_DENIED` | 使用中 | 无通知权限；`show()` 被拒 |
| `1006` | `PAST_TIME_NOT_ALLOWED` | 使用中 | `scheduleAt` 传了过去时间（iOS 明确报错） |
| `1007` | `UNSUPPORTED_INTERVAL` | 使用中 | 间隔不支持（鸿蒙仅 `DAY` / `WEEK`） |
| `1009` | `REMINDER_NOT_ALLOWED` | 使用中 | 代理提醒被管控（鸿蒙需资质；见 [§5](#5-平台差异与已知限制)） |
| `1010` | `UNSUPPORTED` | 使用中 | 该端不支持此能力（如 iOS 调 `createChannel`） |
| `1011` | `INVALID_REQUEST` | 使用中 | 参数非法 / 未知方法 |
| `1003` `1004` `1005` `1008` | `PERMISSION_NOT_DETERMINED` `CHANNEL_NOT_FOUND` `CHANNEL_DISABLED` `BAD_SOUND_FORMAT` | **已预留，当前三端均不会返回** | 常量已定义，供后续细化 |

> 鸿蒙原生错误码会被归一：`1700001` 通知未开启、`1700002` 代理提醒配额/资质不足、`401` 参数错误（多为提前量过短）。
> 其中 `401` **目前按 `INVALID_REQUEST(1011)` 返回**；`1700002` 按 `REMINDER_NOT_ALLOWED(1009)` 返回。

---

### 5. 平台差异与已知限制

| 维度 | Android | iOS | 鸿蒙 |
|---|---|---|---|
| 请求权限 | **<13 无运行时权限，直接返回 `GRANTED` 不弹窗**；13+ 弹框需宿主 `targetSdk ≥ 33`（见 [§3.1](#31-环境要求)）；**拒绝后不再弹** | 首次弹系统框；拒绝后不再弹，需去设置 | 首次弹系统框；拒绝后 `requestEnableNotification` 不再弹，需走设置 |
| 渠道 | `NotificationChannel`，`importance` 可调；**创建后重要性不可修改**；组件会用 `default` 渠道兜底（渠道不存在时自动创建） | 无渠道概念 → `1010` | `addSlot(type)`，**等级由类型固定**：`HIGH→SOCIAL_COMMUNICATION`、`DEFAULT→SERVICE_INFORMATION`、`LOW/MIN→CONTENT_INFORMATION` |
| 横幅 | **仅 `IMPORTANCE_HIGH` 渠道**；已按低重要性建过的渠道无法升级，只能换新 `channelId` | 由系统策略 + `showWhenInForeground` 决定 | 需 `LEVEL_HIGH` 渠道（社交通讯/服务提醒）**且系统「横幅通知」开关打开（默认关闭）** |
| 定时 | `AlarmManager`；精确需宿主声明 `SCHEDULE_EXACT_ALARM` | `UNCalendarNotificationTrigger`；**过去时间 → `1006`** | 代理提醒；**需 `reminder_capability` 资质**，且**提前量 ≥30 秒** |
| 重复 | `setRepeating` | `UNTimeIntervalNotificationTrigger(repeats: true)` | 仅 `DAY` / `WEEK`（Alarm）；其余 → `1007` |
| 角标 | ❌ | ✅（16+ `setBadgeCount`；≤15 `applicationIconBadgeNumber`） | ❌ |
| 前后台判断 | `ProcessLifecycleOwner` | `willPresent` options | `applicationStateManager` |
| 冷启动 payload | `KRNotificationLaunch.onNewIntent()` 缓存 | delegate `didReceive` 自动缓存 | 宿主调 `populateLaunchNotification(want.parameters)` |
| 设置引导 | 4 个接口全支持 | 仅 `openNotificationSettings` | 仅 `openNotificationSettings`（API 13+） |

**明确不支持**：Android 厂商角标；远程/离线推送；厂商推送通道参数配置。

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
| 精确闹钟 `SCHEDULE_EXACT_ALARM` | 宿主 | `scheduleAt` 时间不准 | 宿主在 Manifest 声明（见 [§3.1](#31-环境要求)）；上架 Google Play 需符合其政策 |
| 电池优化白名单 | 宿主 + 用户 | 后台被限制 | 库提供 `isBatteryOptimizationEnabled()` / `openBatteryOptimizationSettings()` |

```kotlin
notification.isBatteryOptimizationEnabled { r -> /* r.data: {"enabled": true} = 电池优化开启（未加白名单） */ }
notification.openBatteryOptimizationSettings { }
notification.openAutoStartSettings { }   // 小米/华为/荣耀/OPPO/vivo/魅族；无对应页面时回退应用详情
notification.openNotificationSettings { }
```

#### 6.2 鸿蒙

| 事项 | 谁负责 | 不做的症状 | 指引 |
|---|---|---|---|
| 代理提醒 `reminder_capability` | 宿主 | `publishReminder` 返回 **`1700002`**（配额 0） | 华为 AppGallery Connect（`developer.huawei.com`）→ 应用 → 配置「代理提醒」相关能力 |
| 系统「横幅通知」开关 | 用户（宿主引导） | 通知只进通知栏，**不弹横幅** | `设置 → 通知和状态栏 → 本应用 → 提醒方式 → 横幅通知`；库提供 `openNotificationSettings()` |

#### 6.3 其他

- **iOS**：无厂商资质要求，权限由系统弹窗授予。
- **Google Play**：`SCHEDULE_EXACT_ALARM` 属敏感权限，上架前确认政策合规。

---

### 7. 宿主接入 Checklist

#### Android

- [ ] Manifest 声明 `POST_NOTIFICATIONS`（精确闹钟再加 `SCHEDULE_EXACT_ALARM`）
- [ ] 宿主入口 Activity `launchMode="singleTask"`
- [ ] `onCreate` / `onNewIntent` 调 `KRNotificationLaunch.onNewIntent(this, intent)`
- [ ] `registerExternalModule` 注册 **`...notification.android.KRNotificationModule`**
- [ ] Kuikly 页面 `createExternalModules()` 注册 **`...notification.module.KRNotificationModule`**
- [ ] 原生侧经 `pageData` 注入 `hostActivity` / `smallIconResId`，页面 `configure()`
- [ ] 需要横幅 → 建 `IMPORTANCE_HIGH` 渠道后再发
- [ ] 国内 ROM：申请/引导厂商白名单与自启动（[§6.1](#61-android)）

#### iOS

- [ ] Podfile 引入 `KuiklyNotificationIOS`（**不要**与已内嵌 KMP 层的静态 framework 重复引）
- [ ] 不要重复设置 `UNUserNotificationCenter.delegate`
- [ ] 页面 `created()` 取 `getLaunchNotification()`

#### 鸿蒙

- [ ] 编出 / 引入 HAR（`cd ohosApp && hvigorw assembleHar`）
- [ ] `module.json5` 声明 `PUBLISH_AGENT_REMINDER`（不声明 `NOTIFICATION_CONTROLLER`）
- [ ] `getCustomRenderModuleCreatorRegisterMap` 注册 Module
- [ ] `configure()` 传 `hostBundleName` + `hostAbilityName`
- [ ] 入口 Ability 调 `populateLaunchNotification(want.parameters)`
- [ ] 定时/重复：申请 `reminder_capability`；提前量 ≥30 秒
- [ ] 横幅：引导用户开系统「横幅通知」
- [ ] **真机验证**（arm64；见 [§9.3](#93-鸿蒙只能跑纯-arkts-验证-demo)）

#### 真机验证清单（三端通用）

- [ ] 前台 / 后台 / 进程被杀 三种状态下的通知展示
- [ ] 点击回跳 + 冷启动 payload
- [ ] 定时 / 重复通知按预期触发
- [ ] 横幅（Android HIGH 渠道 / 鸿蒙系统开关）
- [ ] 角标（iOS）

---

### 8. FAQ / 排障

**Q：调了 `show()` 但通知栏没有？**
1. `checkPermission()` 的 `data.status` 是否为 `GRANTED`？
2. Android：`channelId` 写错了也没关系——组件会用 `default` 渠道兜底（不存在时自动创建）；但要横幅必须用 `IMPORTANCE_HIGH` 渠道。
3. 国内 ROM：是否被厂商管控（小米后台本地通知、华为频次）？→ [§6.1](#61-android)
4. 鸿蒙：只想看横幅？系统「横幅通知」开关默认关闭 → [§6.2](#62-鸿蒙)
5. App 在前台？`showWhenInForeground` 默认 `false`。

**Q：`scheduleAt` 不触发？**
- Android：精确度受 `SCHEDULE_EXACT_ALARM`（宿主声明）与厂商后台策略影响；引导用户加自启动白名单。
- iOS：过去时间返回 `1006`。
- 鸿蒙：需 `reminder_capability`（否则 `1009`）；提前量 <30 秒会返回 `1011`。

**Q：点击通知收不到 payload？**
- Android：入口 Activity 是否 `singleTask`？`onNewIntent` 是否调了 `KRNotificationLaunch.onNewIntent()`？
- 鸿蒙：入口 Ability 是否调了 `populateLaunchNotification(want.parameters)`？
- iOS：是否覆盖了系统 delegate？（不要设）

**Q：`requestPermission` 不弹窗？**
- Android **12 及以下**：系统没有通知运行时权限，直接返回 `GRANTED`，**这是正常的**。
- Android 13+ 但宿主 `targetSdk ≤ 32`：弹框时机由系统控制（通常在首次建渠道时）。
- 任意端**拒绝过之后**：系统不再弹，需引导用户去设置（`openNotificationSettings()`）。

**Q：`createChannel` 返回 `1010`？** 在 iOS 上调用。iOS 没有渠道概念，属预期。

**Q：接入时 IDE 提示 `KRNotificationModule` 找不到 / 导错？** 有两个同名类（包名不同），见 [§3.5 第 4 步](#35-android-接入)。

**Q：鸿蒙模拟器上跑不起来？** 见 [§9.3](#93-鸿蒙只能跑纯-arkts-验证-demo)：**Kuikly 引擎仅 arm64**。

---

### 9. Demo 与验证

#### 9.1 Android（Windows / macOS / Linux 均可编译）

```bash
./gradlew :androidApp:assembleDebug
# 产物：androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

#### 9.2 iOS（需 macOS + Xcode）

见 [`iosApp/README.md`](iosApp/README.md)：创建宿主工程 → 配置 Podfile → `pod install`。

#### 9.3 鸿蒙：只能跑纯 ArkTS 验证 Demo

Kuikly 的鸿蒙渲染引擎 `libkuikly.so` **只有 arm64**（2026-09 核实：ohpm 上的 `@kuikly-open/render` 2.28.0 包内仍只有 `libs/arm64-v8a/`；Maven `-ohos` 构件也只有 `ohosArm64` 变体），而
**Windows / Intel Mac 的鸿蒙模拟器是 x86_64**，且无 ARM 指令翻译层 → **Kuikly 无法在该模拟器运行**。

| 设备 | 架构 | 能跑 Kuikly |
|---|---|---|
| Windows / Intel Mac 鸿蒙模拟器 | x86_64 | ❌ |
| Apple Silicon Mac 鸿蒙模拟器 | arm64 | ✅ |
| 鸿蒙真机 | arm64 | ✅ |

因此本仓库额外提供 **`ohosApp/entry`（纯 ArkTS 验证 Demo）**，不含原生库，可在 x86 模拟器直接验证鸿蒙通知 API（权限 / 渠道 / 立即 / 横幅 / 点击回跳 / 代理提醒 / 取消）。详见 [`ohosApp/README.md`](ohosApp/README.md)。

```bash
cd ohosApp
hvigorw assembleHap --no-daemon
hdc install -r entry/build/default/outputs/default/entry-default-unsigned.hap
```

---

### 10. 版本、兼容性与发布

#### 10.1 版本表

| 项 | 值 |
|---|---|
| 本库 | `1.0.0` |
| Kuikly | `2.24.0-2.1.21` |
| Kotlin | `2.1.21` |
| AGP / Gradle | `8.4.0` / `8.6` |
| 本仓库 demo 的 Android SDK | `compileSdk 34` / `minSdk 21` / `targetSdk 30` |
| iOS | `12.0+` |
| 鸿蒙 | HarmonyOS NEXT（API 26 验证） |

版本集中配置在 [`buildSrc/src/main/java/KotlinBuildVar.kt`](buildSrc/src/main/java/KotlinBuildVar.kt)（单一来源）。

**语义化版本**：遵循 [SemVer](https://semver.org/lang/zh-CN/)。变更记录见 [`CHANGELOG.md`](CHANGELOG.md)。

#### 10.2 鸿蒙产物编译（独立编译链）

鸿蒙产物需用**腾讯定制版 Kotlin 工具链**单独编译（官方 Kotlin 不支持 `ohosArm64`），且 Kuikly 构件要用 `-ohos` 后缀版本，故使用**独立 settings**：

```bash
export OHOS_SDK_HOME="/path/to/DevEco Studio/sdk/default/openharmony"
./gradlew -c settings.ohos.gradle.kts :shared:linkSharedDebugSharedOhosArm64
# 产物：shared/build/bin/ohosArm64/sharedDebugShared/libshared.so + libshared_api.h
```

- `settings.ohos.gradle.kts` / `build.ohos.gradle.kts`：Kotlin `2.0.21-KBA-010` + Kuikly `2.24.0-2.0.21-ohos`
- 与 Android/iOS 使用的 Kotlin `2.1.21` **相互隔离**，互不影响

---

### 11. 隐私与本地数据

组件**不采集、不上报**任何数据，也不加密/改写通知内容。但会在**本机**持久化少量状态（Android 实现，存于 `SharedPreferences`，名 `kr_notification`）：

| 键 | 内容 | 生命周期 |
|---|---|---|
| `host_activity` / `small_icon` | 你在 `configure()` 里传的入口 Activity 与小图标 | 直到被覆盖 |
| `requested` | 是否已请求过通知权限（用于推导 `NOT_DETERMINED`） | 直到应用卸载 |
| `launch_id` / `launch_payload` / `launch_action` | **冷启动 payload（明文）** | `getLaunchNotification()` 消费一次后清除 |
| `scheduled_ids` | 已排期的通知 id（用于 `cancelAll`） | 取消时清除 |

> ⚠️ `payload` 会**明文落盘**直到被消费。若 payload 含敏感信息，请在业务侧自行脱敏或改用可短期失效的引用 id。
> iOS / 鸿蒙实现不落盘 payload（各自随系统 delegate / want 传递）。

---

### 12. 贡献指南

#### 12.1 工程结构

```
KuiklyNotification/            Kuikly 侧（KMP）：Module、数据模型、常量、错误码
KuiklyNotificationAndroid/     Android 实现（AAR）
KuiklyNotificationIOS/         iOS 实现（源码 + podspec）
KuiklyNotificationOhos/        鸿蒙实现（ArkTS HAR）
shared/                        跨端 Demo 页面（@Page("router")）
androidApp/ iosApp/ ohosApp/   三端壳工程
buildSrc/                      版本、坐标、POM 元数据、发布仓库（单一来源）
maven-repo/                    已发布的 Maven 产物（推到 gh-pages 即成为公开仓库）
```

#### 12.2 iOS 两个 podspec

| podspec | 位置 | 用途 |
|---|---|---|
| `KuiklyNotificationIOS` | 仓库根 | **原生实现**，宿主统一引这个 |
| `KuiklyNotification` | `KuiklyNotification/` | KMP 公共 API；仅当宿主**不含** `shared` 这类已内嵌 KMP 层的静态 framework 时才需要（先 `./gradlew :KuiklyNotification:podspec`） |

> 两者同时引入会在链接期产生**大量 duplicate symbols**（曾观察到 >1.5 万条），务必只引其一。

#### 12.3 本地构建

```bash
./gradlew :androidApp:assembleDebug                       # Android（任意 OS）
./gradlew :KuiklyNotification:assemble :KuiklyNotificationAndroid:assemble

./gradlew :shared:generateDummyFramework                   # iOS（macOS）
cd iosApp && pod install

cd ohosApp && hvigorw assembleHar                          # 鸿蒙 HAR

./gradlew -c settings.ohos.gradle.kts :shared:linkSharedDebugSharedOhosArm64   # 鸿蒙 KN 产物
```

#### 12.4 新增一个能力的约定

1. Kuikly 侧 `KuiklyNotification/src/commonMain` 加方法（`KRNotificationModule` 是具体类，不用 expect/actual）
2. 三端各自实现：`KRNotificationModule`(Android) / `.m`(iOS，按方法名反射分发) / `.ets`(鸿蒙，`call()` 分发)
3. **统一回调**：native 回包 `{"code","msg","data"}`，错误码复用 `NotificationConst.ErrorCode`
4. 端不支持的必须回调 `UNSUPPORTED(1010)`，不要静默失败
5. 平台差异登记到本文档 [§5](#5-平台差异与已知限制)
6. **中英双语同步更新**，并在 [`CHANGELOG.md`](CHANGELOG.md) 记录

#### 12.5 提交约定

- 一个提交一个关注点；能编译就本地验证
- 语义化提交：`feat(android):` / `fix(ohos):` / `docs:` / `style(demo):`
- 改了公共 API：必须同时更新文档两语言 + CHANGELOG

#### 12.6 测试

`KuiklyNotification` 已预留 `commonTest`（Kotlin Test）。当前以**三端 Demo + 真机验证**为主（见 [§9](#9-demo-与验证)）。更多约定见 [`CONTRIBUTING.md`](CONTRIBUTING.md)。

---

### 13. License / 支持

- 仓库：<https://github.com/AriaLEntropy/KuiklyNotification>
- 问题反馈：<https://github.com/AriaLEntropy/KuiklyNotification/issues>
- License：Apache-2.0

[⬆ 回到顶部](#kuiklynotification) · [English](README.en.md)
