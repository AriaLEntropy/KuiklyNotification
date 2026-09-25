# ohosApp · 鸿蒙通知验证 Demo（纯 ArkTS）

这是 `KuiklyNotification` 的**鸿蒙测试宿主 / 脚手架**，用途是**直接验证鸿蒙通知 API 的行为**，方便在接入 Kuikly 之前先确认原生侧是否符合预期。

> ⚠️ 它**不是**组件的 Kuikly 实现。组件本体在 `../KuiklyNotificationOhos`（Kuikly 模块），运行它需要 Kuikly 渲染引擎。

## 为什么单独做这个

Kuikly 的鸿蒙渲染引擎 `libkuikly.so` 目前只有 **arm64** 版本，而 Windows / Intel Mac 上的鸿蒙模拟器是 **x86_64**，架构不匹配、跑不起来（指令集层面限制）。

本 Demo **不含任何原生库**（纯 ArkTS），因此可以在 x86 模拟器上运行，用来验证「鸿蒙通知本身」：权限、渠道（Slot）、立即通知、横幅、点击回跳、代理提醒、取消。

## 构建与安装

```bash
# 需要 DevEco 自带的 hvigor（下面路径按实际安装位置调整）
export DEVECO_SDK_HOME="/path/to/DevEco Studio/sdk"
"<DevEco>/tools/hvigor/bin/hvigorw" assembleHap --no-daemon

# 产物：entry/build/default/outputs/default/entry-default-unsigned.hap
hdc install -r entry/build/default/outputs/default/entry-default-unsigned.hap
hdc shell aa start -a EntryAbility -b io.github.arialentropy.notification.demo
```

> 实测：该模拟器接受**未签名** HAP；若你的环境要求签名，请在 DevEco 里配置自动签名。

## 按钮说明

| 按钮 | 验证点 |
|---|---|
| 1. 请求权限 / 2. 查询权限 | `requestEnableNotification` / `isNotificationEnabled` |
| 3. 通知设置 | `openNotificationSettings`（拉起通知管理半模态，可引导开「横幅通知」） |
| 4. 建社交通讯渠道 | `addSlot(SOCIAL_COMMUNICATION)`（= `LEVEL_HIGH`，横幅前提） |
| 5. 发通知(HIGH) / 6. 发通知(低等级) | `publish` + 渠道等级对横幅的影响 |
| 7. 30 秒后提醒 / 8. 每日提醒 | `publishReminder`（Calendar / Alarm） |
| 9. 3 秒后发通知 | 便于先切后台观察横幅 |
| 10. 取消全部 | `cancelAll` + `cancelAllReminders` |

## 实测结论（HarmonyOS 7.0.0 / API 26 模拟器）

| 能力 | 结果 |
|---|---|
| 权限申请 / 查询 | ✅ GRANTED |
| 渠道 `addSlot(SOCIAL_COMMUNICATION)` | ✅ ok（`LEVEL_HIGH`） |
| 立即通知 `publish` | ✅ 通知栏可见 |
| 点击回跳 WantAgent | ✅ `onNewWant` 收到 `payload`，`launchReasonMessage=ReasonMessage_Notification` |
| 取消全部 | ✅ 通知栏清空 |
| **横幅** | ✅ 但**必须**在系统设置里打开「横幅通知」（**默认关闭**）：`设置 → 通知和状态栏 → 本应用 → 提醒方式 → 横幅通知` |
| 定时提醒（提前 10 秒） | ❌ `401 Parameter error`（提前量过短） |
| 定时提醒（提前 30 / 60 秒） | ⚠️ 参数通过，但 `1700002`（配额 0） |
| 每日提醒 | ⚠️ 同上 |

**两条硬约束**（已写入设计文档与 README）：

1. **代理提醒需资质**：定时 / 重复通知需应用具备 `reminder_capability` 云能力（AGC 侧申请），未申请时 `publishReminder` 返回 `1700002`；且提前量建议 ≥1 分钟。
2. **横幅默认不出**：渠道需为社交通讯 / 服务提醒（`LEVEL_HIGH`），且系统「横幅通知」开关默认关闭，需引导用户开启。
