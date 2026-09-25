import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';
import { notificationManager } from '@kit.NotificationKit';
import { reminderAgentManager } from '@kit.BackgroundTasksKit';
import { wantAgent, WantAgent, Want, common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';

const MODULE_NAME: string = 'KRNotificationModule';

const CODE_INVALID_CONFIG: number = 1001;
const CODE_PERMISSION_DENIED: number = 1002;
const CODE_PAST_TIME_NOT_ALLOWED: number = 1006;
const CODE_UNSUPPORTED_INTERVAL: number = 1007;
const CODE_REMINDER_NOT_ALLOWED: number = 1009;
const CODE_UNSUPPORTED: number = 1010;
const CODE_INVALID_REQUEST: number = 1011;

interface KRNotificationParams {
  id?: number;
  title?: string;
  body?: string;
  channelId?: string;
  importance?: string;
  sound?: string;
  payload?: string;
  showWhenInForeground?: boolean;
  interval?: string;
  timestampMs?: number;
  hostBundleName?: string;
  hostAbilityName?: string;
}

/** 统一结果 */
class KRResult {
  code: number = 0;
  msg: string = '';
  data: Object | null = null;

  constructor(code: number, msg: string, data: Object | null) {
    this.code = code;
    this.msg = msg;
    this.data = data;
  }
}

class KRStatusData {
  status: string = '';

  constructor(status: string) {
    this.status = status;
  }
}

class KRCountData {
  count: number = 0;

  constructor(count: number) {
    this.count = count;
  }
}

/**
 * 跨端本地通知模块（鸿蒙实现）。
 *
 * 注册：宿主在 `getCustomRenderModuleCreatorRegisterMap` 中
 * ```
 * map.set(KRNotificationModule.MODULE_NAME, () => new KRNotificationModule(uiAbilityContext))
 * ```
 */
export class KRNotificationModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = MODULE_NAME;

  private context: common.UIAbilityContext;
  private clickCallback: KuiklyRenderCallback | null = null;
  private launchNotification: KRAny = null;
  private reminderMap: Map<number, number> = new Map<number, number>();
  private hostBundleName: string = '';
  private hostAbilityName: string = '';

  constructor(context: common.UIAbilityContext) {
    super();
    this.context = context;
  }

  syncMode(): boolean {
    return true;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case 'configure':
        this.configure(params, callback);
        return null;
      case 'requestPermission':
        this.requestPermission(callback);
        return null;
      case 'checkPermission':
        this.checkPermission(callback);
        return null;
      case 'createChannel':
        this.createChannel(params, callback);
        return null;
      case 'show':
        this.show(params, callback);
        return null;
      case 'scheduleAt':
        this.scheduleAt(params, callback);
        return null;
      case 'showPeriodically':
        this.showPeriodically(params, callback);
        return null;
      case 'cancel':
        this.cancel(params, callback);
        return null;
      case 'cancelAll':
        this.cancelAll(callback);
        return null;
      case 'setBadge':
      case 'getBadge':
        this.respond(callback, this.fail(CODE_UNSUPPORTED, 'unsupported on HarmonyOS'));
        return null;
      // 厂商适配 / 设置引导：鸿蒙只有「通知设置」有意义，其余为 Android 专属
      case 'isBatteryOptimizationEnabled':
        this.respond(callback, this.fail(CODE_UNSUPPORTED, 'unsupported on HarmonyOS'));
        return null;
      case 'openBatteryOptimizationSettings':
        this.respond(callback, this.fail(CODE_UNSUPPORTED, 'unsupported on HarmonyOS'));
        return null;
      case 'openAutoStartSettings':
        this.respond(callback, this.fail(CODE_UNSUPPORTED, 'unsupported on HarmonyOS'));
        return null;
      case 'openNotificationSettings':
        this.openNotificationSettings(callback);
        return null;
      case 'setNotificationClickListener':
        this.clickCallback = callback;
        return null;
      case 'removeNotificationClickListener':
        this.clickCallback = null;
        return null;
      case 'getLaunchNotification':
        return this.getLaunchNotification();
      default:
        this.respond(callback, this.fail(CODE_INVALID_REQUEST, `unknown method: ${method}`));
        return null;
    }
  }

  /** 宿主 Ability 在 onCreate / onNewWant 调用 */
  populateLaunchNotification(parameters: Record<string, Object>): void {
    const id = parameters['id'] as number;
    if (id === undefined) {
      return;
    }
    const payload = parameters['payload'] as string;
    this.launchNotification = new KRLaunchPayload(id, payload ?? '', 'default');
    if (this.clickCallback !== null) {
      this.clickCallback(this.launchNotification);
    }
  }

  // ---------------- 初始化 ----------------

  private configure(params: KRAny, callback: KuiklyRenderCallback | null): void {
    const p = this.parse(params);
    if (p.hostBundleName !== undefined) {
      this.hostBundleName = p.hostBundleName;
    }
    if (p.hostAbilityName !== undefined) {
      this.hostAbilityName = p.hostAbilityName;
    }
    this.respond(callback, this.ok(null));
  }

  // ---------------- 权限 ----------------

  private requestPermission(callback: KuiklyRenderCallback | null): void {
    notificationManager.requestEnableNotification(this.context)
      .then(() => {
        this.respond(callback, this.ok(new KRStatusData('GRANTED')));
      })
      .catch((err: BusinessError) => {
        this.respond(callback, this.ok(new KRStatusData('DENIED')));
      });
  }

  private checkPermission(callback: KuiklyRenderCallback | null): void {
    notificationManager.isNotificationEnabled()
      .then((enabled: boolean) => {
        this.respond(callback, this.ok(new KRStatusData(enabled ? 'GRANTED' : 'DENIED')));
      })
      .catch((err: BusinessError) => {
        this.respond(callback, this.ok(new KRStatusData('DENIED')));
      });
  }

  // ---------------- 渠道（Slot） ----------------

  private createChannel(params: KRAny, callback: KuiklyRenderCallback | null): void {
    const p = this.parse(params);
    notificationManager.addSlot(this.toSlotType(p.importance))
      .then(() => {
        this.respond(callback, this.ok(null));
      })
      .catch((err: BusinessError) => {
        this.respond(callback, this.fail(CODE_INVALID_REQUEST, err.message));
      });
  }

  // ---------------- 发送 ----------------

  private show(params: KRAny, callback: KuiklyRenderCallback | null): void {
    const p = this.parse(params);
    this.buildWantAgent(p).then((agent: WantAgent) => {
      const content: notificationManager.NotificationContent = {
        notificationContentType: notificationManager.ContentType.NOTIFICATION_CONTENT_BASIC_TEXT,
        normal: {
          title: p.title ?? '',
          text: p.body ?? ''
        }
      };
      const request: notificationManager.NotificationRequest = {
        id: p.id ?? 0,
        content: content,
        notificationSlotType: this.toSlotType(p.importance),
        wantAgent: agent
      };
      notificationManager.publish(request)
        .then(() => {
          this.respond(callback, this.ok(null));
        })
        .catch((err: BusinessError) => {
          this.respond(callback, this.fail(CODE_INVALID_REQUEST, err.message));
        });
    }).catch((err: BusinessError) => {
      this.respond(callback, this.fail(CODE_INVALID_REQUEST, err.message));
    });
  }

  private scheduleAt(params: KRAny, callback: KuiklyRenderCallback | null): void {
    const p = this.parse(params);
    if (p.timestampMs === undefined) {
      this.respond(callback, this.fail(CODE_INVALID_REQUEST, 'missing timestampMs'));
      return;
    }
    const date = new Date(p.timestampMs);
    const reminderWantAgent = this.buildReminderWantAgent(p);
    const reminder: reminderAgentManager.ReminderRequestCalendar = {
      reminderType: reminderAgentManager.ReminderType.REMINDER_TYPE_CALENDAR,
      dateTime: {
        year: date.getFullYear(),
        month: date.getMonth() + 1,
        day: date.getDate(),
        hour: date.getHours(),
        minute: date.getMinutes(),
        second: date.getSeconds()
      },
      title: p.title ?? '',
      content: p.body ?? '',
      notificationId: p.id ?? 0,
      slotType: this.toSlotType(p.importance),
      wantAgent: reminderWantAgent
    };
    reminderAgentManager.publishReminder(reminder)
      .then((reminderId: number) => {
        this.reminderMap.set(p.id ?? 0, reminderId);
        this.respond(callback, this.ok(null));
      })
      .catch((err: BusinessError) => {
        this.respond(callback, this.fail(CODE_REMINDER_NOT_ALLOWED, err.message));
      });
  }

  private showPeriodically(params: KRAny, callback: KuiklyRenderCallback | null): void {
    const p = this.parse(params);
    const interval = p.interval ?? '';
    if (interval !== 'DAY' && interval !== 'WEEK') {
      this.respond(callback, this.fail(CODE_UNSUPPORTED_INTERVAL, `unsupported interval: ${interval}`));
      return;
    }
    const now = new Date();
    const daysOfWeek: number[] = interval === 'WEEK'
      ? [now.getDay() === 0 ? 7 : now.getDay()]
      : [1, 2, 3, 4, 5, 6, 7];
    const reminderWantAgent = this.buildReminderWantAgent(p);
    const reminder: reminderAgentManager.ReminderRequestAlarm = {
      reminderType: reminderAgentManager.ReminderType.REMINDER_TYPE_ALARM,
      hour: now.getHours(),
      minute: now.getMinutes(),
      daysOfWeek: daysOfWeek,
      title: p.title ?? '',
      content: p.body ?? '',
      notificationId: p.id ?? 0,
      slotType: this.toSlotType(p.importance),
      wantAgent: reminderWantAgent
    };
    reminderAgentManager.publishReminder(reminder)
      .then((reminderId: number) => {
        this.reminderMap.set(p.id ?? 0, reminderId);
        this.respond(callback, this.ok(null));
      })
      .catch((err: BusinessError) => {
        this.respond(callback, this.fail(CODE_REMINDER_NOT_ALLOWED, err.message));
      });
  }

  // ---------------- 取消 ----------------

  private cancel(params: KRAny, callback: KuiklyRenderCallback | null): void {
    const p = this.parse(params);
    const id = p.id ?? 0;
    notificationManager.cancel(id);
    const reminderId = this.reminderMap.get(id);
    if (reminderId !== undefined) {
      reminderAgentManager.cancelReminder(reminderId).catch((err: BusinessError) => {
      });
      this.reminderMap.delete(id);
    }
    this.respond(callback, this.ok(null));
  }

  private cancelAll(callback: KuiklyRenderCallback | null): void {
    notificationManager.cancelAll();
    reminderAgentManager.cancelAllReminders().catch((err: BusinessError) => {
    });
    this.reminderMap.clear();
    this.respond(callback, this.ok(null));
  }

  // ---------------- 设置引导 ----------------

  /** 拉起本应用的通知管理半模态（可用于引导用户开启通知 / 横幅通知）。API 13+ */
  private openNotificationSettings(callback: KuiklyRenderCallback | null): void {
    notificationManager.openNotificationSettings(this.context)
      .then(() => {
        this.respond(callback, this.ok(null));
      })
      .catch((err: BusinessError) => {
        this.respond(callback, this.fail(CODE_INVALID_REQUEST, err.message));
      });
  }

  // ---------------- 冷启动（同步） ----------------

  private getLaunchNotification(): KRAny {
    const result = this.launchNotification;
    this.launchNotification = null;
    if (result === null) {
      return null;
    }
    return JSON.stringify(result);
  }

  // ---------------- 内部工具 ----------------

  /** 立即通知使用 AbilityKit WantAgent */
  private async buildWantAgent(p: KRNotificationParams): Promise<WantAgent> {
    const parameters: Record<string, Object> = {
      'payload': p.payload ?? '',
      'id': p.id ?? 0
    };
    const want: Want = {
      bundleName: this.hostBundleName,
      abilityName: this.hostAbilityName,
      parameters: parameters
    };
    const info: wantAgent.WantAgentInfo = {
      wants: [want],
      actionType: wantAgent.OperationType.START_ABILITY,
      requestCode: p.id ?? 0,
      actionFlags: [wantAgent.WantAgentFlags.UPDATE_PRESENT_FLAG]
    };
    return await wantAgent.getWantAgent(info);
  }

  /** 定时/重复提醒使用 ReminderAgent 自带的 WantAgent 结构 */
  private buildReminderWantAgent(p: KRNotificationParams): reminderAgentManager.WantAgent {
    const parameters: Record<string, Object> = {
      'payload': p.payload ?? '',
      'id': p.id ?? 0
    };
    const reminderWantAgent: reminderAgentManager.WantAgent = {
      pkgName: this.hostBundleName,
      abilityName: this.hostAbilityName,
      parameters: parameters
    };
    return reminderWantAgent;
  }

  private toSlotType(importance?: string): notificationManager.SlotType {
    if (importance === 'HIGH') {
      return notificationManager.SlotType.SOCIAL_COMMUNICATION;
    }
    if (importance === 'DEFAULT') {
      return notificationManager.SlotType.SERVICE_INFORMATION;
    }
    return notificationManager.SlotType.CONTENT_INFORMATION;
  }

  private parse(params: KRAny): KRNotificationParams {
    if (typeof params === 'string') {
      try {
        return JSON.parse(params) as KRNotificationParams;
      } catch (e) {
        return {};
      }
    }
    if (params !== null && typeof params === 'object') {
      return params as KRNotificationParams;
    }
    return {};
  }

  private ok(data: Object | null): KRResult {
    return new KRResult(0, '', data);
  }

  private fail(code: number, msg: string): KRResult {
    return new KRResult(code, msg, null);
  }

  private respond(callback: KuiklyRenderCallback | null, result: KRResult): void {
    if (callback !== null) {
      callback(result);
    }
  }
}

/** 冷启动 / 点击事件 payload */
class KRLaunchPayload {
  id: number = 0;
  payload: string = '';
  action: string = 'default';

  constructor(id: number, payload: string, action: string) {
    this.id = id;
    this.payload = payload;
    this.action = action;
  }
}
