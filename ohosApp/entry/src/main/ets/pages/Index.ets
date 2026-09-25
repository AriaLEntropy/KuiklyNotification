import { common } from '@kit.AbilityKit';
import { NotificationHelper } from '../notification/NotificationHelper';

@Entry
@Component
struct Index {
  @State logs: string[] = [];
  @StorageLink('launchParams') launchParams: string = '';
  private helper: NotificationHelper | null = null;

  aboutToAppear(): void {
    const ctx: common.UIAbilityContext = getContext(this) as common.UIAbilityContext;
    this.helper = new NotificationHelper(ctx, (line: string) => {
      this.pushLog(line);
    });
    this.pushLog(`host: ${this.helper.hostInfo()}`);
  }

  private pushLog(line: string): void {
    this.logs = [`${new Date().toLocaleTimeString()}  ${line}`].concat(this.logs);
  }

  build() {
    Column({ space: 6 }) {
      Text('KuiklyNotification · 鸿蒙通知验证（纯 ArkTS）')
        .fontSize(17).fontWeight(FontWeight.Bold).width('100%')
      Text('本页不含原生库，可在 x86 模拟器运行；用于验证鸿蒙通知 API 行为（Kuikly 引擎为 arm64，无法在 x86 模拟器运行）。')
        .fontSize(11).fontColor('#888888').width('100%')
      Text(`点击回跳：${this.launchParams === '' ? '（暂无）' : this.launchParams}`)
        .fontSize(11).fontColor('#1a6fd4').width('100%')

      Scroll() {
        Column({ space: 2 }) {
          ForEach(this.logs, (item: string, index: number) => {
            Text(item).fontSize(11).width('100%').textAlign(TextAlign.Start)
          }, (item: string, index: number) => `${index}_${item}`)
        }.width('100%')
      }
      .layoutWeight(1).width('100%').backgroundColor('#F5F5F5').padding(6)

      Row({ space: 6 }) {
        Button('1. 请求权限').layoutWeight(1).fontSize(12)
          .onClick(() => { this.helper?.requestPermission(); })
        Button('2. 查询权限').layoutWeight(1).fontSize(12)
          .onClick(() => { this.helper?.checkPermission(); })
      }.width('100%')

      Row({ space: 6 }) {
        Button('3. 通知设置').layoutWeight(1).fontSize(12)
          .onClick(() => { this.helper?.openNotificationSettings(); })
        Button('4. 建社交通讯渠道').layoutWeight(1).fontSize(11)
          .onClick(() => { this.helper?.addSlot(true); })
      }.width('100%')

      Row({ space: 6 }) {
        Button('5. 发通知(HIGH)').layoutWeight(1).fontSize(11)
          .onClick(() => { this.helper?.show(1, '立即通知', '社交通讯渠道，开「横幅通知」后弹横幅', true, 'p_high'); })
        Button('6. 发通知(低等级)').layoutWeight(1).fontSize(11)
          .onClick(() => { this.helper?.show(2, '低等级通知', '内容资讯渠道，不弹横幅', false, 'p_low'); })
      }.width('100%')

      Row({ space: 6 }) {
        Button('7. 30 秒后提醒').layoutWeight(1).fontSize(11)
          .onClick(() => { this.helper?.scheduleAt(3, '定时提醒', '30 秒后的代理提醒', 30000); })
        Button('8. 每日提醒').layoutWeight(1).fontSize(11)
          .onClick(() => { this.helper?.showDaily(4, '每日提醒', '每日代理提醒(Alarm)'); })
      }.width('100%')

      Row({ space: 6 }) {
        Button('9. 3 秒后发通知').layoutWeight(1).fontSize(11)
          .onClick(() => {
            setTimeout(() => {
              this.helper?.show(5, '延迟通知', '可先切到后台观察横幅', true, 'p_delayed');
            }, 3000);
          })
        Button('10. 取消全部').layoutWeight(1).fontSize(11)
          .onClick(() => { this.helper?.cancelAll(); })
      }.width('100%')

      Button('清空日志').width('100%').fontSize(12).onClick(() => { this.logs = []; })
    }
    .width('100%').height('100%').padding(10)
  }
}
