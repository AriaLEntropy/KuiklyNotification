#import <Foundation/Foundation.h>
#import <OpenKuiklyIOSRender/KRBaseModule.h>

NS_ASSUME_NONNULL_BEGIN

/// 跨端本地通知模块（iOS 实现）
@interface KRNotificationModule : KRBaseModule

/// 供 KRNotificationDelegate 分发点击事件
- (void)handleClickEvent:(NSDictionary *)event;

@end

NS_ASSUME_NONNULL_END
