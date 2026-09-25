#import "KRNotificationDelegate.h"
#import "KRNotificationModule.h"

static NSString *const kKRUserInfoId = @"id";
static NSString *const kKRUserInfoPayload = @"payload";
static NSString *const kKRUserInfoShowInForeground = @"kr_show_in_foreground";

@implementation KRNotificationDelegate {
    NSDictionary *_pendingLaunch;
}

+ (instancetype)shared {
    static KRNotificationDelegate *instance;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[KRNotificationDelegate alloc] init];
    });
    return instance;
}

+ (void)setupIfNeeded {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        [UNUserNotificationCenter currentNotificationCenter].delegate = [KRNotificationDelegate shared];
    });
}

+ (void)load {
    [self setupIfNeeded];
}

- (nullable NSDictionary *)consumeLaunchPayload {
    NSDictionary *payload = _pendingLaunch;
    _pendingLaunch = nil;
    return payload;
}

#pragma mark - UNUserNotificationCenterDelegate

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler {
    BOOL showInForeground = [notification.request.content.userInfo[kKRUserInfoShowInForeground] boolValue];
    if (!showInForeground) {
        completionHandler(UNNotificationPresentationOptionNone);
        return;
    }
    if (@available(iOS 14.0, *)) {
        completionHandler(UNNotificationPresentationOptionBanner |
                          UNNotificationPresentationOptionSound |
                          UNNotificationPresentationOptionBadge);
    } else {
        completionHandler(UNNotificationPresentationOptionAlert |
                          UNNotificationPresentationOptionSound |
                          UNNotificationPresentationOptionBadge);
    }
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
didReceiveNotificationResponse:(UNNotificationResponse *)response
         withCompletionHandler:(void (^)(void))completionHandler {
    NSDictionary *userInfo = response.notification.request.content.userInfo;
    NSDictionary *event = @{
        kKRUserInfoId: userInfo[kKRUserInfoId] ?: @0,
        kKRUserInfoPayload: userInfo[kKRUserInfoPayload] ?: @"",
        @"action": response.actionIdentifier ?: UNNotificationDefaultActionIdentifier
    };

    KRNotificationModule *module = self.module;
    if (module) {
        [module handleClickEvent:event];
    } else {
        // 冷启动：Module 尚未创建，先缓存，等 getLaunchNotification 取用
        _pendingLaunch = event;
    }
    completionHandler();
}

@end
