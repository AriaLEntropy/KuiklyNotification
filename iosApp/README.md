# iosApp（iOS 宿主工程说明）

> Windows 上无法生成 Xcode 工程（`.xcodeproj`），请在 Mac 上按下述步骤接入。
> 组件本体已在 `KuiklyNotificationIOS/` 与 KMP 模块 `KuiklyNotification/` 中实现。

## 1. 创建 iOS 宿主工程

用 Kuikly 的 iOS 集成方式创建（Kuikly 模板工程的 `iosApp`，或按官方《iOS KuiklyRender 接入》文档），
得到一个能加载 Kuikly 页面的 iOS App。

## 2. 配置 Podfile

分两种宿主，按需选择（**不要同时引**，否则会 duplicate symbols）：

### A. Demo 宿主（含 `shared` demo 页面，本仓库示例）

`shared` 的静态 framework 已经**把 KMP 层（`KuiklyNotification`）合并进自身**，
因此只需引组件原生实现 + 渲染库，**不要再引** `pod 'KuiklyNotification'`：

```ruby
platform :ios, '12.0'
use_frameworks!

target 'iosApp' do
  # 组件原生实现（本仓库根目录）
  pod 'KuiklyNotificationIOS', :path => '../'
  # Kuikly iOS 渲染库（版本与宿主 Kuikly 一致）
  pod 'OpenKuiklyIOSRender', '~> 2.24.0'
end
```

### B. 独立宿主（只引 KMP 层、不含 demo 页）

此时才需要单独引入 KMP 层：

```ruby
  pod 'KuiklyNotification', :path => '../KuiklyNotification'   # 先执行 ./gradlew :KuiklyNotification:podspec
```

> 实测：在 A 方案里同时引 `KuiklyNotification` 会产生约 15585 个 duplicate symbols。

然后：

```bash
pod install
```

## 3. 加载页面

Demo 页面用 `@Page("router")` 注册（见 `shared/.../NotificationDemoPage.kt`），
宿主启动 Kuikly 页面时使用页面名 `router`。

## 4. 通知能力无需额外配置

- 组件在 `+load` 阶段自动设置 `UNUserNotificationCenter.delegate`（`KRNotificationDelegate`），
  宿主**不要**重复设置。
- 冷启动拉起 payload 由 `KRNotificationDelegate` 在 `didReceive` 中缓存，页面 `created()` 里
  `getLaunchNotification()` 取一次。
- iOS 16+ 角标用 `setBadgeCount`，低版本 fallback `applicationIconBadgeNumber`。

## 5. 验证清单

- [ ] `pod install` 成功
- [ ] 请求权限弹窗出现
- [ ] 立即/定时/重复通知能弹出
- [ ] 点击通知回到 App，listener 收到 payload
- [ ] 杀进程后点击通知冷启动，`getLaunchNotification()` 能取到
- [ ] iOS 16+ / ≤15 角标分支
