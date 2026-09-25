# KuiklyNotification

**English** | [中文](README.md)

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Release](https://img.shields.io/badge/release-v1.0.0-orange.svg)](CHANGELOG.md)
[![Kuikly](https://img.shields.io/badge/Kuikly-2.24.0--2.1.21-4C8BF5.svg)](https://github.com/Tencent-TDS/KuiklyUI)
[![Android](https://img.shields.io/badge/Android-minSdk%2021-3DDC84.svg)](#platform-requirements)
[![iOS](https://img.shields.io/badge/iOS-12%2B-lightgrey.svg)](#platform-requirements)
[![HarmonyOS](https://img.shields.io/badge/HarmonyOS-NEXT-black.svg)](#platform-requirements)

A cross-platform **local notification** component for [Kuikly](https://github.com/Tencent-TDS/KuiklyUI)
(Android / iOS / HarmonyOS). One Kotlin codebase to post notifications in the system tray, with permission
handling, channels, immediate/scheduled/repeating notifications, cancel, tap-to-open, cold-start payload
and badges (iOS).

> Glossary: 通知渠道 = Notification Channel / Slot · 横幅 = Heads-up / Banner · 代理提醒 = Agent-powered Reminder · 冷启动 = Cold start

## Table of contents

1. [What it is / is not](#1-what-it-is--is-not)
2. [Capability matrix](#2-capability-matrix)
3. [Getting started](#3-getting-started)
4. [API reference](#4-api-reference)
5. [Platform differences & limitations](#5-platform-differences--limitations)
6. [Vendor qualifications](#6-vendor-qualifications)
7. [Host integration checklist](#7-host-integration-checklist)
8. [FAQ / Troubleshooting](#8-faq--troubleshooting)
9. [Demos & verification](#9-demos--verification)
10. [Versions, compatibility & releasing](#10-versions-compatibility--releasing)
11. [Privacy & local data](#11-privacy--local-data)
12. [Contributing](#12-contributing)
13. [License / Support](#13-license--support)

---

### 1. What it is / is not

**It is** a Kuikly component for showing **local notifications** (initiated by the app), plus permission
handling, channels, tap-to-open and cold-start payload.

**It is not**:

- ❌ **It does not obtain vendor qualifications for you.** Chinese OEMs (Xiaomi/Huawei/Honor/OPPO/vivo/Meizu) and
  HarmonyOS agent-powered reminders require qualifications the **host app must apply for**; the library only
  provides **settings-guidance APIs** (see [§6](#6-vendor-qualifications)).
- ❌ **No remote push.** Offline push needs vendor push channels (a separate component).
- ❌ **No OEM badges.** Android has no unified badge API (iOS badges are supported).
- ❌ **No encryption/rewriting** of content. Treat title/body/payload as potentially sensitive
  (see [§11 Privacy & local data](#11-privacy--local-data)).

---

### 2. Capability matrix

| Capability | Android | iOS | HarmonyOS |
|---|---|---|---|
| Request / check permission | ✅ | ✅ | ✅ |
| Notification channel | ✅ adjustable importance | ❌ `UNSUPPORTED(1010)` | ⚠️ level fixed by SlotType |
| Show / cancel / cancelAll | ✅ | ✅ | ✅ |
| Scheduled notification | ✅ (host must declare the exact-alarm permission) | ✅ (past time errors) | ⚠️ needs qualification + lead time ≥30 s |
| Repeating notification | ✅ | ✅ | ⚠️ `DAY` / `WEEK` only |
| Tap-to-open (persistent listener) | ✅ | ✅ | ✅ |
| Cold-start payload | ✅ | ✅ | ⚠️ host must inject the want |
| Badge | ❌ | ✅ | ❌ |
| Settings guidance | ✅ 4 APIs | ⚠️ notification settings only | ⚠️ notification settings only (API 13+) |

---

### 3. Getting started

#### 3.1 Platform requirements

**Host app requirements**

| Item | Requirement | Notes |
|---|---|---|
| Kuikly | `2.24.0-2.1.21` (match your app) | artifacts from Tencent's public read-only mirror |
| Kotlin | `2.1.21` | KMP module compile version |
| Android | `minSdk 21`, `compileSdk ≥ 34` | **`targetSdk` is the host's choice**, see below |
| iOS | `12.0+` | — |
| HarmonyOS | HarmonyOS NEXT | verified on API 26 in this repo |

**Two Android prerequisites that are easy to miss**

1. **The dialog from `requestPermission` requires the host to set `targetSdk ≥ 33`.** On Android 13+:
   - host `targetSdk ≥ 33`: calling `requestPermission` shows the system dialog;
   - host `targetSdk ≤ 32`: the system prompts **when the first notification channel is created**, and the
     behaviour of `requestPermission` is controlled by the system.
   (This repo's demo uses `targetSdk 30` for demonstration only — it is not a library requirement.)
2. **The exact-alarm permission must be declared by the host** — the library manifest does not include it:
   ```xml
   <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
   ```
   Without it notifications still work (inexact alarms), but timing may drift.

> This repo's own build versions (Kuikly / Kotlin / AGP / Gradle) are listed in [§10.1](#101-version-table).

#### 3.2 Adding the dependency

> **Repository visibility**: this repository is currently **private**. The Maven / CocoaPods `:git` paths below
> **are configured and locally verified**, but external developers can only fetch them **once the repository is
> public**. Until then, use *Option B: build from source*.

**Option A: Maven repository (recommended; requires a public repo)**

Artifacts are published to this repository's `gh-pages` branch (standard Maven layout). Pick either URL:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        // (1) plain raw URL — no setup needed
        maven { url = uri("https://raw.githubusercontent.com/AriaLEntropy/KuiklyNotification/gh-pages/") }
        // (2) or, after enabling Settings → Pages on the gh-pages branch:
        // maven { url = uri("https://arialentropy.github.io/KuiklyNotification/") }
        // Kuikly runtime
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
    }
}

// host module build.gradle.kts
dependencies {
    implementation("io.github.arialentropy:KuiklyNotification:1.0.0")         // Kuikly-side public API
    implementation("io.github.arialentropy:KuiklyNotificationAndroid:1.0.0")  // Android native implementation
}
```

**Option B: build from source (always works)**

```bash
git clone https://github.com/AriaLEntropy/KuiklyNotification.git

# B1. composite build — add to the host settings.gradle.kts
includeBuild("../KuiklyNotification")
#     then keep the same coordinates above (Gradle substitutes the local project)

# B2. or publish to your local Maven repo first, then add mavenLocal() in the host
./gradlew :KuiklyNotification:publishToMavenLocal :KuiklyNotificationAndroid:publishToMavenLocal
```

**iOS (CocoaPods)**

```ruby
# once the repo is public: reference the git tag (recommended)
pod 'KuiklyNotificationIOS', :git => 'https://github.com/AriaLEntropy/KuiklyNotification.git', :tag => '1.0.0'

# local development / while the repo is private
pod 'KuiklyNotificationIOS', :path => '../KuiklyNotification'

pod 'OpenKuiklyIOSRender', '~> 2.24.0'   # match your Kuikly version
```

> ⚠️ Don't pick the wrong podspec (see [§12.2](#122-ios-two-podspecs)):
> - `KuiklyNotificationIOS` (repo root) → **native implementation**, what hosts normally need;
> - `KuiklyNotification` (in `KuiklyNotification/`) → **KMP public API**, only needed by a standalone host that
>   does *not* link a static framework already embedding the KMP layer (such as `shared`).

**HarmonyOS (HAR)**

ohpm cannot reference git directly, so build the HAR first:

```bash
cd ohosApp                       # ← must run inside ohosApp
hvigorw assembleHar
# output: KuiklyNotificationOhos/build/default/outputs/default/KuiklyNotificationOhos.har
```

```json5
// host oh-package.json5
{
  "dependencies": {
    "@xiaoaiechan/kuikly-notification-ohos": "file:../KuiklyNotificationOhos"
  }
}
```

> If it is later published to the ohpm registry, this becomes `ohpm install @xiaoaiechan/kuikly-notification-ohos`
> (the ecosystem convention, see [§12.3](#123-release-flow-maintainers)).

#### 3.3 Common step: `configure`

`NotificationConfig` is injected by the **native side** through `pageData` when opening the Kuikly page; the page
reads it and calls `configure`:

```kotlin
// (1) native side (Android example: KuiklyRenderActivity.createPageData())
pageData["hostActivity"] = KuiklyRenderActivity::class.java.name
pageData["smallIconResId"] = android.R.drawable.ic_dialog_info   // or your own R.drawable.xxx

// (2) Kuikly page (commonMain): read and pass to the component
import com.tencent.kuikly.core.pager.Pager
import io.github.arialentropy.notification.model.NotificationConfig
import io.github.arialentropy.notification.module.KRNotificationModule

val notification = acquireModule<KRNotificationModule>(KRNotificationModule.MODULE_NAME)
val params = pageData.params
notification.configure(
    NotificationConfig(
        hostActivity = params.optString("hostActivity").takeIf { it.isNotEmpty() },
        smallIconResId = params.optInt("smallIconResId").takeIf { it != 0 },
        hostBundleName = params.optString("hostBundleName").takeIf { it.isNotEmpty() },   // HarmonyOS
        hostAbilityName = params.optString("hostAbilityName").takeIf { it.isNotEmpty() }  // HarmonyOS
    )
) { result -> /* result.code == 0 means success */ }
```

> ⚠️ **Never write `R.drawable.xxx` inside a Kuikly page**: `R` is Android-only and is not visible from
> `commonMain` (shared Kotlin code) — it will not compile. Pass it from the native side via `pageData` as shown above.

| Parameter | Platform | Required | Notes |
|---|---|---|---|
| `hostActivity` | Android | ✅ | Launcher Activity **FQCN** (tap target) |
| `smallIconResId` | Android | ✅ | Notification small icon resource id |
| `hostBundleName` | HarmonyOS | ✅ | App bundleName |
| `hostAbilityName` | HarmonyOS | ✅ | Entry ability name |

> iOS needs no `configure` (delegate based). Calling it on iOS is a safe no-op.

#### 3.4 Minimal runnable example

```kotlin
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.module.Module
import io.github.arialentropy.notification.demo.BasePager          // or your own Pager
import io.github.arialentropy.notification.model.NotificationRequest
import io.github.arialentropy.notification.module.KRNotificationModule

@Page("router")
internal class MyPage : BasePager() {

    private val notification: KRNotificationModule
        get() = acquireModule<KRNotificationModule>(KRNotificationModule.MODULE_NAME)

    // (1) register the module (per Pager)
    override fun createExternalModules(): Map<String, Module>? =
        hashMapOf(KRNotificationModule.MODULE_NAME to KRNotificationModule())

    override fun created() {
        super.created()

        // (2) permission
        notification.requestPermission { result -> /* data.status = GRANTED / DENIED */ }

        // (3) channel (Android needs a HIGH channel for heads-up)
        notification.createChannel("high", "High priority", "HIGH") { }

        // (4) show immediately
        notification.show(
            NotificationRequest(id = 1, title = "Title", body = "Body", channelId = "high", payload = "p1")
        ) { result -> /* result.code == 0 */ }

        // (5) tap listener + cold start
        notification.setNotificationClickListener { event -> /* event.id / event.payload */ }
        notification.getLaunchNotification()?.let { event -> /* cold start */ }
    }

    override fun body(): ViewBuilder = { /* page content */ }
}
```

#### 3.5 Android integration

**1) Manifest permission**

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<!-- optional: exact alarms (see §3.1) -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

**2) Launcher Activity `singleTask`** (otherwise a tap creates a new instance while the app is alive)

```xml
<activity android:name=".MainActivity" android:launchMode="singleTask" android:exported="true" />
```

**3) Report the Intent in `onCreate` / `onNewIntent`**

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

**4) Register the native module**

```kotlin
// mind the two same-named classes (different packages):
//   io.github.arialentropy.notification.android.KRNotificationModule   ← native implementation (here)
//   io.github.arialentropy.notification.module.KRNotificationModule    ← Kuikly-side API (used in pages)
import io.github.arialentropy.notification.android.KRNotificationModule
import com.tencent.kuikly.core.render.android.IKuiklyRenderExport

override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
    super.registerExternalModule(kuiklyRenderExport)
    with(kuiklyRenderExport) {
        moduleExport(KRNotificationModule.MODULE_NAME) { KRNotificationModule() }
    }
}
```

> Scheduled/repeating notifications are delivered by an internal `BroadcastReceiver`; no host declaration needed.

#### 3.6 iOS integration

1. Podfile → see [§3.2](#32-adding-the-dependency) (avoid duplicates, see [§12.2](#122-ios-two-podspecs)).
2. **Do not set** `UNUserNotificationCenter.delegate` — the component sets it in `+load`.
3. In the page's `created()`, call `getLaunchNotification()` and register `setNotificationClickListener`.

#### 3.7 HarmonyOS integration

**1) `module.json5` permission**

```json5
"requestPermissions": [
  { "name": "ohos.permission.PUBLISH_AGENT_REMINDER" }   // for scheduled/repeating
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

> ⚠️ The Kuikly HarmonyOS **engine** (`libkuikly.so`) is **arm64 only**; Windows / Intel Mac HarmonyOS emulators
> are x86_64 and **cannot run Kuikly** (see [§9.3](#93-harmonyos-pure-arkts-verification-demo-only)).

#### 3.8 Self-check

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

**`NotificationConfig`**: `hostActivity`(Android) / `smallIconResId`(Android) / `hostBundleName`(HarmonyOS) / `hostAbilityName`(HarmonyOS)

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

**`NotificationClickEvent`**: `id: Int` / `payload: String?` / `action: String` (reserved, default `"default"`)

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

- `code == 0` means success; otherwise see [§4.4](#44-error-codes)
- `data` shapes are listed in [§4.1](#41-methods); most methods return `{}`
- **`checkPermission` reports the status in `data.status`** (`GRANTED` / `DENIED` / `NOT_DETERMINED`), **not** via `code`
- `getLaunchNotification()` is **synchronous** and returns `NotificationClickEvent?` — not a `JsonResult`

#### 4.4 Error codes

| code | Constant | Status | Meaning / common cause |
|---|---|---|---|
| `0` | `SUCCESS` | in use | Success |
| `-1` | `FAILED` | in use | Unexpected native reply (rare) |
| `1001` | `INVALID_CONFIG` | in use | `configure` missing / required params absent |
| `1002` | `PERMISSION_DENIED` | in use | No notification permission; `show()` rejected |
| `1006` | `PAST_TIME_NOT_ALLOWED` | in use | `scheduleAt` in the past (explicit error on iOS) |
| `1007` | `UNSUPPORTED_INTERVAL` | in use | Unsupported interval (HarmonyOS: `DAY`/`WEEK` only) |
| `1009` | `REMINDER_NOT_ALLOWED` | in use | Agent reminder restricted (HarmonyOS needs qualification) |
| `1010` | `UNSUPPORTED` | in use | Not supported on this platform (e.g. `createChannel` on iOS) |
| `1011` | `INVALID_REQUEST` | in use | Invalid parameters / unknown method |
| `1003` `1004` `1005` `1008` | `PERMISSION_NOT_DETERMINED` `CHANNEL_NOT_FOUND` `CHANNEL_DISABLED` `BAD_SOUND_FORMAT` | **reserved — not returned by any platform today** | constants exist for future refinement |

> HarmonyOS native codes are normalized: `1700001` notifications off, `1700002` reminder quota/qualification,
> `401` invalid parameter (usually lead time too short).
> `401` is currently returned as `INVALID_REQUEST(1011)`; `1700002` as `REMINDER_NOT_ALLOWED(1009)`.

---

### 5. Platform differences & limitations

| Aspect | Android | iOS | HarmonyOS |
|---|---|---|---|
| Request permission | **<13: no runtime permission, returns `GRANTED` without dialog**; on 13+ the dialog requires host `targetSdk ≥ 33` (see [§3.1](#31-platform-requirements)); **never again after denial** | dialog on first call; after denial go to Settings | dialog on first call; after denial `requestEnableNotification` no longer shows; use Settings |
| Channels | `NotificationChannel` with adjustable importance; **immutable after creation**; the component falls back to `default` (auto-created if missing) | no channels → `1010` | `addSlot(type)`, **level fixed by type**: `HIGH→SOCIAL_COMMUNICATION`, `DEFAULT→SERVICE_INFORMATION`, `LOW/MIN→CONTENT_INFORMATION` |
| Heads-up | **`IMPORTANCE_HIGH` channels only**; an existing low-importance channel cannot be upgraded — use a new `channelId` | system policy + `showWhenInForeground` | needs a `LEVEL_HIGH` channel **and** the system "banner" toggle (off by default) |
| Scheduled | `AlarmManager`; exact requires the host to declare `SCHEDULE_EXACT_ALARM` | `UNCalendarNotificationTrigger`; **past time → `1006`** | agent reminder; **needs `reminder_capability`** and **lead time ≥30 s** |
| Repeating | `setRepeating` | `UNTimeIntervalNotificationTrigger(repeats: true)` | `DAY`/`WEEK` only; others → `1007` |
| Badge | ❌ | ✅ (16+ `setBadgeCount`; ≤15 `applicationIconBadgeNumber`) | ❌ |
| Foreground detection | `ProcessLifecycleOwner` | `willPresent` options | `applicationStateManager` |
| Cold-start payload | `KRNotificationLaunch.onNewIntent()` | cached by the delegate | host calls `populateLaunchNotification(want.parameters)` |
| Settings guidance | all 4 APIs | `openNotificationSettings` only | `openNotificationSettings` only (API 13+) |

**Explicitly not supported**: Android OEM badges; remote/offline push; vendor push channel configuration.

---

### 6. Vendor qualifications

> **The library does not obtain qualifications for you.** The host app must apply/configure the items below.
> Vendor portals change frequently — **refer to the official, latest documentation**.

#### 6.1 Android

| Item | Owner | Symptom if missing | Where to look |
|---|---|---|---|
| Xiaomi "post local notifications in background" whitelist | Host | background local notifications are **not shown** | Xiaomi Open Platform (`dev.mi.com`); or switch to Xiaomi Push |
| Huawei / Honor "message self-classification" entitlement | Host | notifications throttled as marketing | Huawei AppGallery Connect (`developer.huawei.com`) → app → message classification |
| Auto-start / background whitelist | Host + user | scheduled/repeating notifications **do not fire** after the app is killed | the library exposes `openAutoStartSettings()` |
| Exact alarm `SCHEDULE_EXACT_ALARM` | Host | `scheduleAt` inaccurate | declare it in the host manifest (see [§3.1](#31-platform-requirements)); check Google Play policy |
| Battery optimization whitelist | Host + user | background restricted | `isBatteryOptimizationEnabled()` / `openBatteryOptimizationSettings()` |

```kotlin
notification.isBatteryOptimizationEnabled { r -> /* r.data: {"enabled": true} = optimization on (not whitelisted) */ }
notification.openBatteryOptimizationSettings { }
notification.openAutoStartSettings { }   // Xiaomi/Huawei/Honor/OPPO/vivo/Meizu; falls back to app details
notification.openNotificationSettings { }
```

#### 6.2 HarmonyOS

| Item | Owner | Symptom if missing | Where to look |
|---|---|---|---|
| Agent reminder `reminder_capability` | Host | `publishReminder` returns **`1700002`** (quota 0) | Huawei AppGallery Connect (`developer.huawei.com`) → app → agent reminder capability |
| System "banner notification" toggle | User (guide them) | only in the tray, **no heads-up** | `Settings → Notifications → this app → alert style → banner`; the library exposes `openNotificationSettings()` |

#### 6.3 Others

- **iOS**: no vendor qualification; the system prompts for permission.
- **Google Play**: `SCHEDULE_EXACT_ALARM` is a sensitive permission — verify policy compliance before release.

---

### 7. Host integration checklist

#### Android

- [ ] Manifest: `POST_NOTIFICATIONS` (plus `SCHEDULE_EXACT_ALARM` for exact alarms)
- [ ] Launcher Activity `launchMode="singleTask"`
- [ ] `KRNotificationLaunch.onNewIntent(this, intent)` in `onCreate` / `onNewIntent`
- [ ] `registerExternalModule` registers **`...notification.android.KRNotificationModule`**
- [ ] Kuikly page `createExternalModules()` registers **`...notification.module.KRNotificationModule`**
- [ ] Native side injects `hostActivity` / `smallIconResId` via `pageData`; page calls `configure()`
- [ ] For heads-up: create an `IMPORTANCE_HIGH` channel first
- [ ] Chinese OEMs: apply for / guide users through vendor whitelists ([§6.1](#61-android))

#### iOS

- [ ] Podfile adds `KuiklyNotificationIOS` (**do not** also add a static framework embedding the KMP layer)
- [ ] Do **not** set `UNUserNotificationCenter.delegate`
- [ ] Call `getLaunchNotification()` in the page's `created()`

#### HarmonyOS

- [ ] Build / reference the HAR (`cd ohosApp && hvigorw assembleHar`)
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
1. Is `checkPermission()`'s `data.status` `GRANTED`?
2. Android: a wrong `channelId` is not fatal — the component falls back to the `default` channel (auto-created);
   but heads-up requires an `IMPORTANCE_HIGH` channel.
3. Chinese OEMs: vendor controls may block background local notifications → [§6.1](#61-android)
4. HarmonyOS heads-up: the system "banner" toggle is off by default → [§6.2](#62-harmonyos)
5. Is the app foreground? `showWhenInForeground` defaults to `false`.

**Q: `scheduleAt` never fires.**
- Android: exactness depends on `SCHEDULE_EXACT_ALARM` (declared by the host) and OEM background policy.
- iOS: past timestamps return `1006`.
- HarmonyOS: needs `reminder_capability` (otherwise `1009`); lead times below 30 s return `1011`.

**Q: Tap does not deliver the payload.**
- Android: is the launcher Activity `singleTask`? Is `KRNotificationLaunch.onNewIntent()` called from `onNewIntent`?
- HarmonyOS: did the entry ability call `populateLaunchNotification(want.parameters)`?
- iOS: did you override the system delegate? (don't)

**Q: `requestPermission` shows no dialog.**
- Android **12 and below**: no runtime permission exists; it returns `GRANTED` — **expected**.
- Android 13+ with host `targetSdk ≤ 32`: the system controls the timing (usually at first channel creation).
- Any platform **after a denial**: the system will not prompt again; guide the user to Settings via `openNotificationSettings()`.

**Q: `createChannel` returns `1010`.** Called on iOS. iOS has no channels — expected.

**Q: My IDE can't find / imports the wrong `KRNotificationModule`.** There are two same-named classes — see
[§3.5 step 4](#35-android-integration).

**Q: It fails to run on a HarmonyOS emulator.** See [§9.3](#93-harmonyos-pure-arkts-verification-demo-only):
**the Kuikly engine is arm64 only**.

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

The Kuikly HarmonyOS render engine `libkuikly.so` is **arm64 only** (verified 2026-09: the `@kuikly-open/render`
2.28.0 package on ohpm still ships only `libs/arm64-v8a/`, and the Maven `-ohos` artifacts only expose the
`ohosArm64` variant). Windows / Intel Mac HarmonyOS emulators are **x86_64** with no ARM translation layer →
**Kuikly cannot run there**.

| Device | ABI | Runs Kuikly |
|---|---|---|
| Windows / Intel Mac HarmonyOS emulator | x86_64 | ❌ |
| Apple Silicon Mac HarmonyOS emulator | arm64 | ✅ |
| HarmonyOS phone | arm64 | ✅ |

This repo therefore also ships **`ohosApp/entry` (a pure-ArkTS verification demo)** with no native libraries,
runnable on an x86_64 emulator to verify HarmonyOS notification APIs (permission / channel / immediate / heads-up /
tap / agent reminder / cancel). See [`ohosApp/README.md`](ohosApp/README.md).

```bash
cd ohosApp
hvigorw assembleHap --no-daemon
hdc install -r entry/build/default/outputs/default/entry-default-unsigned.hap
```

---

### 10. Versions, compatibility & releasing

#### 10.1 Version table

| Item | Value |
|---|---|
| This library | `1.0.0` |
| Kuikly | `2.24.0-2.1.21` |
| Kotlin | `2.1.21` |
| AGP / Gradle | `8.4.0` / `8.6` |
| This repo's demo Android SDK | `compileSdk 34` / `minSdk 21` / `targetSdk 30` |
| iOS | `12.0+` |
| HarmonyOS | HarmonyOS NEXT (verified API 26) |

Versions live in [`buildSrc/src/main/java/KotlinBuildVar.kt`](buildSrc/src/main/java/KotlinBuildVar.kt) (single source of truth).

**Semantic versioning**: this project follows [SemVer](https://semver.org/). See [`CHANGELOG.md`](CHANGELOG.md).

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

### 11. Privacy & local data

The component **collects and reports nothing**, and does not encrypt/rewrite content. It does persist a small
amount of state on device (Android implementation, in `SharedPreferences` named `kr_notification`):

| Key | Content | Lifetime |
|---|---|---|
| `host_activity` / `small_icon` | the launcher Activity and small icon you passed to `configure()` | until overwritten |
| `requested` | whether the permission was requested (used to derive `NOT_DETERMINED`) | until uninstall |
| `launch_id` / `launch_payload` / `launch_action` | **cold-start payload (plaintext)** | cleared once `getLaunchNotification()` consumes it |
| `scheduled_ids` | scheduled notification ids (for `cancelAll`) | cleared on cancel |

> ⚠️ `payload` is written to disk **in plaintext** until consumed. If it contains sensitive data, redact it in
> your business layer or use a short-lived reference id instead.
> The iOS / HarmonyOS implementations do not persist the payload (it travels via the system delegate / want).

---

### 12. Contributing

#### 12.1 Project layout

```
KuiklyNotification/            Kuikly side (KMP): module, models, constants, error codes
KuiklyNotificationAndroid/     Android implementation (AAR)
KuiklyNotificationIOS/         iOS implementation (sources + podspec)
KuiklyNotificationOhos/        HarmonyOS implementation (ArkTS HAR)
shared/                        Cross-platform demo page (@Page("router"))
androidApp/ iosApp/ ohosApp/   Host shells
buildSrc/                      versions, coordinates, POM metadata, publish repositories
maven-repo/                    published Maven artifacts (push to gh-pages to serve publicly)
```

#### 12.2 iOS: two podspecs

| podspec | Location | Purpose |
|---|---|---|
| `KuiklyNotificationIOS` | repo root | **native implementation** — what hosts normally use |
| `KuiklyNotification` | `KuiklyNotification/` | KMP public API; only for hosts that do **not** link a static framework embedding the KMP layer (run `./gradlew :KuiklyNotification:podspec` first) |

> Adding both produces a large number of **duplicate symbols** at link time (>15k observed). Always pick one.

#### 12.3 Release flow (maintainers)

Maven artifacts (KMP + Android):

```bash
# Linux / macOS
./publish-maven.sh
# Windows
.\gradlew.bat :KuiklyNotification:publishAllPublicationsToLocalRepoRepository `
              :KuiklyNotificationAndroid:publishReleasePublicationToLocalRepoRepository

# push to the gh-pages branch to serve a public Maven repository
git add maven-repo && git commit -m "chore(release): publish maven artifacts"
git subtree push --prefix maven-repo origin gh-pages
```

- Coordinates: `io.github.arialentropy:KuiklyNotification` / `KuiklyNotificationAndroid`
- Repository: `maven-repo/` in this repo → `gh-pages` branch (defined in `Publishing.registerRepositories`)
- If Sonatype credentials exist (`username` / `password` in `local.properties`), a `central` repository is added as well
- **Prerequisite**: the repository must be **public** for external developers to fetch

**HarmonyOS HAR → ohpm registry** (prepared; an account is required)

```bash
# One-time setup (see https://ohpm.openharmony.cn)
#   1) register with a Huawei account and create an organization/scope
#      The personal account scope IS the username (this package uses @xiaoaiechan);
#      a custom scope (e.g. @arialentropy) requires an organization, which needs enterprise verification
#   2) generate a publish_id and an SSH key pair: upload the public key, keep the private key locally
#   3) write ~/.ohpm/.ohpmrc:
#        publish_registry=https://ohpm.openharmony.cn/ohpm/
#        publish_id=<your-publish-id>
#        key_path=<path-to-private-key>

./publish-ohpm.sh      # = build HAR → ohpm prepublish → ohpm publish
# without an account you can still run the local pre-check: ohpm prepublish <har>
```

- Package name: `@xiaoaiechan/kuikly-notification-ohos`
- The published artifact is the HAR produced by `hvigorw assembleHar`; `README.md` / `LICENSE` in the module
  directory are packed into it
- Bump `KuiklyNotificationOhos/oh-package.json5`'s `version` on every release
- The pre-check warns that the HAR contains source code — that is expected (a HAR ships ArkTS source; switch to a
  bytecode HAR if obfuscation is required)
- After publishing, consumers can simply run `ohpm install @xiaoaiechan/kuikly-notification-ohos`

#### 12.4 Build locally

```bash
./gradlew :androidApp:assembleDebug                       # Android (any OS)
./gradlew :KuiklyNotification:assemble :KuiklyNotificationAndroid:assemble

./gradlew :shared:generateDummyFramework                   # iOS (macOS)
cd iosApp && pod install

cd ohosApp && hvigorw assembleHar                          # HarmonyOS HAR

./gradlew -c settings.ohos.gradle.kts :shared:linkSharedDebugSharedOhosArm64   # HarmonyOS KN artifact
```

#### 12.5 Adding a capability

1. Add the method on the Kuikly side (`KuiklyNotification/src/commonMain`; `KRNotificationModule` is a concrete class)
2. Implement on all three platforms (`KRNotificationModule`(Android) / `.m`(iOS, selector dispatch) / `.ets`(HarmonyOS, `call()`)
3. **Uniform reply**: `{"code","msg","data"}`, reuse `NotificationConst.ErrorCode`
4. Unsupported platforms **must** reply `UNSUPPORTED(1010)` — never fail silently
5. Register platform differences in [§5](#5-platform-differences--limitations)
6. **Update both languages of this README** and add a [`CHANGELOG.md`](CHANGELOG.md) entry

#### 12.6 Commit conventions

- One concern per commit; verify locally whenever it compiles
- Conventional commits: `feat(android):` / `fix(ohos):` / `docs:` / `style(demo):`
- Public API changes require updating both languages of this README and the CHANGELOG

#### 12.7 Tests

`KuiklyNotification` reserves `commonTest` (Kotlin Test). Verification is currently driven by the three demos plus
real-device testing (see [§9](#9-demos--verification)). More details in [`CONTRIBUTING.md`](CONTRIBUTING.md).

---

### 13. License / Support

- Repository: <https://github.com/AriaLEntropy/KuiklyNotification>
- Issues: <https://github.com/AriaLEntropy/KuiklyNotification/issues>
- License: Apache-2.0

[⬆ Back to top](#kuiklynotification) · [中文](README.md)
