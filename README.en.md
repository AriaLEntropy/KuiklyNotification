# KuiklyNotification

A cross-platform **local notification** component for [Kuikly](https://github.com/Tencent-TDS/KuiklyUI) (Android / iOS / HarmonyOS).

`v1.0.0` · Apache-2.0 · [中文](README.md)

> Glossary: 通知渠道 = Notification Channel / Slot · 横幅 = Heads-up / Banner · 代理提醒 = Agent-powered Reminder · 冷启动 = Cold start

---

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
| --- | --- | --- | --- |
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
| --- | --- |
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
| --- | --- | --- | --- |
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
| --- | --- | --- | --- | --- | --- |
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
| --- | --- | --- |
| `hostActivity` | `String?` | Android launcher Activity (FQCN) |
| `smallIconResId` | `Int?` | Android small icon resource id |
| `hostBundleName` | `String?` | HarmonyOS bundleName |
| `hostAbilityName` | `String?` | HarmonyOS entry ability name |

**`NotificationRequest`**

| Field | Type | Default | Notes |
| --- | --- | --- | --- |
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
| --- | --- | --- |
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
| --- | --- | --- |
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
| --- | --- | --- | --- |
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
| --- | --- | --- | --- |
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
| --- | --- | --- | --- |
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
| --- | --- | --- |
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
| --- | --- |
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

`[⬆ Back to top](#kuiklynotification)`
