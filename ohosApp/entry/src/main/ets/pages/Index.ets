import { common } from '@kit.AbilityKit';
import { NotificationHelper } from '../notification/NotificationHelper';

const PAGE_BG: string = '#F2F3F5';
const CARD_BG: string = '#FFFFFF';
const TEXT_MAIN: string = '#1F2329';
const TEXT_SUB: string = '#8A9099';
const LOG_BG: string = '#1E1E1E';
const LOG_TEXT: string = '#9CDCFE';

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

  @Builder
  SectionLabel(title: string) {
    Text(title)
      .fontSize(12)
      .fontColor(TEXT_SUB)
      .width('100%')
      .margin({ top: 4, bottom: 8 })
  }

  @Builder
  DemoButton(title: string, action: () => void) {
    Row() {
      Text(title).fontSize(13).fontColor(TEXT_MAIN)
    }
    .width('100%')
    .height(46)
    .backgroundColor(CARD_BG)
    .borderRadius(10)
    .justifyContent(FlexAlign.Center)
    .margin({ bottom: 8 })
    .onClick(() => { action(); })
  }

  build() {
    Column() {
      // 头部卡片
      Column() {
        Text('KuiklyNotification · 鸿蒙通知验证')
          .fontSize(17).fontWeight(FontWeight.Bold).fontColor(TEXT_MAIN).width('100%')
        Text('本页不含原生库，可在 x86 模拟器运行；用于验证鸿蒙通知 API 行为。')
          .fontSize(11).fontColor(TEXT_SUB).width('100%').margin({ top: 4 })
        Text(`点击回跳：${this.launchParams === '' ? '（暂无）' : this.launchParams}`)
          .fontSize(11).fontColor('#1A6FD4').width('100%').margin({ top: 4 })
          .maxLines(3).textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .width('100%')
      .backgroundColor(CARD_BG)
      .borderRadius(12)
      .padding(14)
      .margin({ bottom: 12 })

      Scroll() {
        Column() {
          this.SectionLabel('操作')

          this.DemoButton('1. 请求权限', () => { this.helper?.requestPermission(); })
          this.DemoButton('2. 查询权限', () => { this.helper?.checkPermission(); })
          this.DemoButton('3. 通知设置（引导开「横幅通知」）', () => { this.helper?.openNotificationSettings(); })
          this.DemoButton('4. 建社交通讯渠道（LEVEL_HIGH）', () => { this.helper?.addSlot(true); })
          this.DemoButton('5. 发通知(HIGH，出横幅)', () => {
            this.helper?.show(1, '立即通知', '社交通讯渠道，开「横幅通知」后弹横幅', true, 'p_high');
          })
          this.DemoButton('6. 发通知(低等级，不出横幅)', () => {
            this.helper?.show(2, '低等级通知', '内容资讯渠道，不弹横幅', false, 'p_low');
          })
          this.DemoButton('7. 30 秒后提醒（代理提醒）', () => {
            this.helper?.scheduleAt(3, '定时提醒', '30 秒后的代理提醒', 30000);
          })
          this.DemoButton('8. 每日提醒（代理提醒）', () => {
            this.helper?.showDaily(4, '每日提醒', '每日代理提醒(Alarm)');
          })
          this.DemoButton('9. 3 秒后发通知（可先切后台）', () => {
            setTimeout(() => {
              this.helper?.show(5, '延迟通知', '可先切到后台观察横幅', true, 'p_delayed');
            }, 3000);
          })
          this.DemoButton('10. 取消全部', () => { this.helper?.cancelAll(); })

          this.SectionLabel('日志')

          // 日志卡片
          Column() {
            if (this.logs.length === 0) {
              Text('（操作结果会显示在这里）').fontSize(11).fontColor(LOG_TEXT).width('100%')
            }
            ForEach(this.logs, (item: string, index: number) => {
              Text(item)
                .fontSize(11).fontColor(LOG_TEXT).width('100%')
                .lineHeight(16).textAlign(TextAlign.Start)
            }, (item: string, index: number) => `${index}_${item}`)
          }
          .width('100%')
          .backgroundColor(LOG_BG)
          .borderRadius(10)
          .padding(10)
          .margin({ bottom: 8 })
        }
        .width('100%')
      }
      .layoutWeight(1)
      .width('100%')
      .scrollBar(BarState.Off)
      .align(Alignment.Top)

      Row() {
        Button('清空日志')
          .fontSize(12)
          .fontColor(TEXT_SUB)
          .backgroundColor('#E8EAED')
          .borderRadius(10)
          .width('100%')
          .height(40)
          .onClick(() => { this.logs = []; })
      }
      .width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(PAGE_BG)
    .padding({ top: 12, left: 16, right: 16, bottom: 12 })
  }
}
