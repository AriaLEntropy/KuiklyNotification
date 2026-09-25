import { AbilityConstant, UIAbility, Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';

const DOMAIN: number = 0x0000;
const TAG: string = 'KRNotificationDemo';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    this.recordWant('onCreate', want);
  }

  onNewWant(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    this.recordWant('onNewWant', want);
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        hilog.error(DOMAIN, TAG, 'loadContent failed: %{public}s', JSON.stringify(err));
      }
    });
  }

  /**
   * 记录点击通知回跳时带回来的参数（冷启动 onCreate / 已存活 onNewWant），
   * 用于验证 WantAgent 点击是否能带回 payload。
   */
  private recordWant(from: string, want: Want): void {
    let params: string = '{}';
    if (want !== undefined && want !== null && want.parameters !== undefined) {
      params = JSON.stringify(want.parameters);
    }
    hilog.info(DOMAIN, TAG, '%{public}s want=%{public}s', from, params);
    AppStorage.setOrCreate<string>('launchParams', `${from}: ${params}`);
  }
}
