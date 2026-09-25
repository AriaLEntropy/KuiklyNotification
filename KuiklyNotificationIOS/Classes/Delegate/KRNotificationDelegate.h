#import <Foundation/Foundation.h>
#import <UserNotifications/UserNotifications.h>

NS_ASSUME_NONNULL_BEGIN

@class KRNotificationModule;

/// UNUserNotificationCenter 代理：处理前台展示策略、点击事件与冷启动拉起
@interface KRNotificationDelegate : NSObject <UNUserNotificationCenterDelegate>

/// 当前存活的 Module（弱引用）
@property (nonatomic, weak, nullable) KRNotificationModule *module;

+ (instancetype)shared;

/// 在最早期时机设置 UNUserNotificationCenter.delegate
+ (void)setupIfNeeded;

/// 取出并清除冷启动拉起 payload（一次性）
- (nullable NSDictionary *)consumeLaunchPayload;

@end

NS_ASSUME_NONNULL_END
