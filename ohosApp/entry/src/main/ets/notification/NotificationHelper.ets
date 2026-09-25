import { notificationManager } from '@kit.NotificationKit';
import { reminderAgentManager } from '@kit.BackgroundTasksKit';
import { wantAgent, WantAgent, Want, common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';

/**
 * 纯 ArkTS 的鸿蒙通知封装，逻辑与 KuiklyNotificationOhos 的 KRNotificationModule.ets 对齐。
 *
 * 为什么单独存在：
 *   Kuikly 的鸿蒙渲染引擎（libkuikly.so）目前只有 arm64 版本，无法在 Windows / Intel Mac 的
 *   x86_64 鸿蒙模拟器上运行。这个 Demo 不含任何原生库，因此可以在 x86 模拟器上直接验证
 *   「鸿蒙通知本身」的行为：权限、渠道(Slot)、立即通知、横幅、点击回跳、代理提醒、取消。
 */
export class NotificationHelper {
  private context: common.UIAbilityContext;
  private bundleName: string;
  private abilityName: string;
  private reminderMap: Map<number, number> = new Map<number, number>();
  private log: (line: string) => void;

  constructor(context: common.UIAbilityContext, log: (line: string) => void) {
    this.context = context;
    this.bundleName = context.abilityInfo.bundleName;
    this.abilityName = context.abilityInfo.name;
    this.log = log;
  }

  hostInfo(): string {
    return `${this.bundleName} / ${this.abilityName}`;
  }

  // ---------------- 权限 ----------------

  async requestPermission(): Promise<void> {
    try {
      await notificationManager.requestEnableNotification(this.context);
      this.log('requestEnableNotification -> GRANTED');
    } catch (e) {
      const err = e as BusinessError;
      this.log(`requestEnableNotification -> DENIED (${err.code})`);
    }
  }

  async checkPermission(): Promise<void> {
    try {
      const enabled: boolean = await notificationManager.isNotificationEnabled();
      this.log(`isNotificationEnabled -> ${enabled ? 'GRANTED' : 'DENIED'}`);
    } catch (e) {
      const err = e as BusinessError;
      this.log(`isNotificationEnabled error: ${err.code} ${err.message}`);
    }
  }

  /** 拉起通知管理半模态：可引导用户开启「允许通知」与「横幅通知」。API 13+ */
  async openNotificationSettings(): Promise<void> {
    try {
      await notificationManager.openNotificationSettings(this.context);
      this.log('openNotificationSettings -> ok');
    } catch (e) {
      const err = e as BusinessError;
      this.log(`openNotificationSettings error: ${err.code} ${err.message}`);
    }
  }

  // ---------------- 渠道（Slot） ----------------

  /** 鸿蒙等级由 SlotType 固定：SOCIAL_COMMUNICATION / SERVICE_INFORMATION = LEVEL_HIGH（可出横幅） */
  async addSlot(high: boolean): Promise<void> {
    const type: notificationManager.SlotType = high
      ? notificationManager.SlotType.SOCIAL_COMMUNICATION
      : notificationManager.SlotType.CONTENT_INFORMATION;
    try {
      await notificationManager.addSlot(type);
      this.log(`addSlot -> ok (${high ? 'SOCIAL_COMMUNICATION/LEVEL_HIGH' : 'CONTENT_INFORMATION/LEVEL_MIN'})`);
    } catch (e) {
      const err = e as BusinessError;
      this.log(`addSlot error: ${err.code} ${err.message}`);
    }
  }

  // ---------------- 立即通知 ----------------

  async show(id: number, title: string, body: string, high: boolean, payload: string): Promise<void> {
    const type: notificationManager.SlotType = high
      ? notificationManager.SlotType.SOCIAL_COMMUNICATION
      : notificationManager.SlotType.CONTENT_INFORMATION;
    try {
      const content: notificationManager.NotificationContent = {
        notificationContentType: notificationManager.ContentType.NOTIFICATION_CONTENT_BASIC_TEXT,
        normal: { title: title, text: body }
      };
      const request: notificationManager.NotificationRequest = {
        id: id,
        content: content,
        notificationSlotType: type,
        wantAgent: await this.buildWantAgent(id, payload)
      };
      await notificationManager.publish(request);
      this.log(`publish -> ok id=${id}${high ? ' (HIGH 渠道：设置里开「横幅通知」后弹横幅)' : ' (低等级渠道：不出横幅)'}`);
    } catch (e) {
      const err = e as BusinessError;
      this.log(`publish error: ${err.code} ${err.message}`);
    }
  }

  // ---------------- 代理提醒（定时 / 重复） ----------------

  /**
   * 定时提醒（Calendar）。
   * 注意：① 需应用具备 reminder_capability 资质，否则返回 1700002；
   *      ② 提前量不能过短，实测 10 秒 → 401，30 秒 / 60 秒通过。
   */
  async scheduleAt(id: number, title: string, body: string, delayMs: number): Promise<void> {
    try {
      const d: Date = new Date(Date.now() + delayMs);
      const reminder: reminderAgentManager.ReminderRequestCalendar = {
        reminderType: reminderAgentManager.ReminderType.REMINDER_TYPE_CALENDAR,
        dateTime: {
          year: d.getFullYear(),
          month: d.getMonth() + 1,
          day: d.getDate(),
          hour: d.getHours(),
          minute: d.getMinutes(),
          second: d.getSeconds()
        },
        title: title,
        content: body,
        notificationId: id,
        slotType: notificationManager.SlotType.SOCIAL_COMMUNICATION,
        wantAgent: this.buildReminderWantAgent(id, `calendar_payload_${id}`)
      };
      const reminderId: number = await reminderAgentManager.publishReminder(reminder);
      this.reminderMap.set(id, reminderId);
      this.log(`publishReminder(calendar) -> ok reminderId=${reminderId}`);
    } catch (e) {
      const err = e as BusinessError;
      this.log(`publishReminder(calendar) error: ${err.code} ${err.message}`);
    }
  }

  /** 每日重复提醒（Alarm） */
  async showDaily(id: number, title: string, body: string): Promise<void> {
    try {
      const now: Date = new Date();
      const reminder: reminderAgentManager.ReminderRequestAlarm = {
        reminderType: reminderAgentManager.ReminderType.REMINDER_TYPE_ALARM,
        hour: now.getHours(),
        minute: now.getMinutes(),
        daysOfWeek: [1, 2, 3, 4, 5, 6, 7],
        title: title,
        content: body,
        notificationId: id,
        slotType: notificationManager.SlotType.SOCIAL_COMMUNICATION,
        wantAgent: this.buildReminderWantAgent(id, `daily_payload_${id}`)
      };
      const reminderId: number = await reminderAgentManager.publishReminder(reminder);
      this.reminderMap.set(id, reminderId);
      this.log(`publishReminder(alarm/daily) -> ok reminderId=${reminderId}`);
    } catch (e) {
      const err = e as BusinessError;
      this.log(`publishReminder(alarm) error: ${err.code} ${err.message}`);
    }
  }

  // ---------------- 取消 ----------------

  async cancelAll(): Promise<void> {
    try {
      notificationManager.cancelAll();
      await reminderAgentManager.cancelAllReminders();
      this.reminderMap.clear();
      this.log('cancelAll -> ok');
    } catch (e) {
      const err = e as BusinessError;
      this.log(`cancelAll error: ${err.code} ${err.message}`);
    }
  }

  // ---------------- 内部工具 ----------------

  private async buildWantAgent(id: number, payload: string): Promise<WantAgent> {
    const parameters: Record<string, Object> = { 'payload': payload, 'id': id };
    const want: Want = {
      bundleName: this.bundleName,
      abilityName: this.abilityName,
      parameters: parameters
    };
    const info: wantAgent.WantAgentInfo = {
      wants: [want],
      actionType: wantAgent.OperationType.START_ABILITY,
      requestCode: id,
      actionFlags: [wantAgent.WantAgentFlags.UPDATE_PRESENT_FLAG]
    };
    return await wantAgent.getWantAgent(info);
  }

  private buildReminderWantAgent(id: number, payload: string): reminderAgentManager.WantAgent {
    const parameters: Record<string, Object> = { 'payload': payload, 'id': id };
    const agent: reminderAgentManager.WantAgent = {
      pkgName: this.bundleName,
      abilityName: this.abilityName,
      parameters: parameters
    };
    return agent;
  }
}
