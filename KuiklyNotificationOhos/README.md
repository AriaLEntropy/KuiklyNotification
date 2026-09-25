# kuikly-notification-ohos

KuiklyNotification 的**鸿蒙（HarmonyOS）原生实现**（ArkTS HAR），属于
[KuiklyNotification](https://github.com/AriaLEntropy/KuiklyNotification) 组件，配合
[Kuikly](https://github.com/Tencent-TDS/KuiklyUI) 使用。

在 Android / iOS / 鸿蒙三端提供统一的本地通知能力：权限、通知渠道、立即/定时/重复通知、取消、
点击回跳、冷启动 payload。

## 安装

```bash
ohpm install kuikly-notification-ohos
```

> 若尚未发布到 ohpm 中心仓，可先用本地 HAR：`"kuikly-notification-ohos": "file:../KuiklyNotificationOhos"`

## 依赖

- `@kuikly-open/render`（Kuikly 鸿蒙渲染层）

## 使用

```ts
import { KRNotificationModule } from 'kuikly-notification-ohos';

// 在 Kuikly 视图 delegate 中注册
getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator> {
  const map: Map<string, KRRenderModuleExportCreator> = new Map();
  map.set(KRNotificationModule.MODULE_NAME, () => new KRNotificationModule(this.uiAbilityContext));
  return map;
}
```

`module.json5` 需声明：

```json5
"requestPermissions": [
  { "name": "ohos.permission.PUBLISH_AGENT_REMINDER" }   // 定时/重复通知需要
]
```

## 已知限制

- **定时 / 重复通知**依赖代理提醒，需应用具备 `reminder_capability` 云能力（AGC 侧申请），否则返回 `1700002`；
  且提前量需 ≥30 秒。
- **横幅通知**需要系统设置里开启「横幅通知」（默认关闭），可引导用户到
  `设置 → 通知和状态栏 → 本应用 → 提醒方式 → 横幅通知`。
- 通知渠道等级由 SlotType 固定（`HIGH→SOCIAL_COMMUNICATION`、`DEFAULT→SERVICE_INFORMATION`、
  `LOW/MIN→CONTENT_INFORMATION`）。

## License

Apache-2.0
