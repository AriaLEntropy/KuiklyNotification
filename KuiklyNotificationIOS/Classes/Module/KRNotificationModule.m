#import "KRNotificationModule.h"
#import "KRNotificationDelegate.h"

#import <UserNotifications/UserNotifications.h>
#import <UIKit/UIKit.h>

#pragma mark - Helpers

static NSDictionary *KROkResult(void) {
    return @{ @"code": @0, @"msg": @"", @"data": @{} };
}

static NSDictionary *KROkData(NSDictionary *data) {
    return @{ @"code": @0, @"msg": @"", @"data": data ?: @{} };
}

static NSDictionary *KRFailResult(NSInteger code, NSString *msg) {
    return @{ @"code": @(code), @"msg": msg ?: @"", @"data": [NSNull null] };
}

static void KRInvoke(KuiklyRenderCallback callback, NSDictionary *result) {
    if (!callback) { return; }
    if ([NSThread isMainThread]) {
        callback(result);
    } else {
        dispatch_async(dispatch_get_main_queue(), ^{
            callback(result);
        });
    }
}

static NSDictionary *KRParams(id param) {
    if ([param isKindOfClass:[NSDictionary class]]) { return param; }
    if ([param isKindOfClass:[NSString class]]) {
        NSString *json = (NSString *)param;
        if (json.length == 0) { return @{}; }
        NSData *data = [json dataUsingEncoding:NSUTF8StringEncoding];
        id obj = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
        if ([obj isKindOfClass:[NSDictionary class]]) { return obj; }
    }
    return @{};
}

static NSTimeInterval KRIntervalSeconds(NSString *interval) {
    if ([interval isEqualToString:@"MINUTE"]) { return 60; }
    if ([interval isEqualToString:@"HOUR"]) { return 3600; }
    if ([interval isEqualToString:@"HALF_DAY"]) { return 43200; }
    if ([interval isEqualToString:@"DAY"]) { return 86400; }
    if ([interval isEqualToString:@"WEEK"]) { return 604800; }
    return -1;
}

static NSString *KRIdentifier(NSDictionary *params) {
    return [NSString stringWithFormat:@"kr_notif_%@", params[@"id"] ?: @0];
}

#pragma mark - Module

@interface KRNotificationModule ()

@property (nonatomic, copy, nullable) KuiklyRenderCallback clickCallback;

@end

@implementation KRNotificationModule

#pragma mark 初始化

- (void)configure:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    [KRNotificationDelegate setupIfNeeded];
    KRInvoke(callback, KROkResult());
}

#pragma mark 权限

- (void)requestPermission:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    [KRNotificationDelegate setupIfNeeded];
    UNAuthorizationOptions options =
        UNAuthorizationOptionAlert | UNAuthorizationOptionSound | UNAuthorizationOptionBadge;
    [[UNUserNotificationCenter currentNotificationCenter]
        requestAuthorizationWithOptions:options
                      completionHandler:^(BOOL granted, NSError * _Nullable error) {
        NSString *status = error ? @"ERROR" : (granted ? @"GRANTED" : @"DENIED");
        KRInvoke(callback, KROkData(@{ @"status": status }));
    }];
}

- (void)checkPermission:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    [[UNUserNotificationCenter currentNotificationCenter]
        getNotificationSettingsWithCompletionHandler:^(UNNotificationSettings *settings) {
        NSString *status;
        switch (settings.authorizationStatus) {
            case UNAuthorizationStatusAuthorized:
            case UNAuthorizationStatusProvisional:
            case UNAuthorizationStatusEphemeral:
                status = @"GRANTED";
                break;
            case UNAuthorizationStatusNotDetermined:
                status = @"NOT_DETERMINED";
                break;
            default:
                status = @"DENIED";
                break;
        }
        KRInvoke(callback, KROkData(@{ @"status": status }));
    }];
}

#pragma mark 渠道（iOS 无渠道概念）

- (void)createChannel:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    KRInvoke(callback, KRFailResult(1010, @"createChannel is unsupported on iOS"));
}

#pragma mark 发送

- (UNNotificationRequest *)buildRequestWithParams:(NSDictionary *)params
                                          trigger:(UNNotificationTrigger *)trigger {
    UNMutableNotificationContent *content = [[UNMutableNotificationContent alloc] init];
    content.title = params[@"title"] ?: @"";
    content.body = params[@"body"] ?: @"";
    content.userInfo = @{
        @"id": params[@"id"] ?: @0,
        @"payload": params[@"payload"] ?: @"",
        @"kr_show_in_foreground": params[@"showWhenInForeground"] ?: @NO
    };
    NSString *sound = params[@"sound"];
    if ([sound isKindOfClass:[NSString class]] && sound.length > 0) {
        content.sound = [UNNotificationSound soundNamed:sound];
    }
    NSNumber *badge = params[@"badge"];
    if ([badge isKindOfClass:[NSNumber class]]) {
        content.badge = badge;
    }
    return [UNNotificationRequest requestWithIdentifier:KRIdentifier(params)
                                                content:content
                                                trigger:trigger];
}

- (void)addRequest:(NSDictionary *)params
           trigger:(UNNotificationTrigger *)trigger
          callback:(KuiklyRenderCallback)callback {
    UNNotificationRequest *request = [self buildRequestWithParams:params trigger:trigger];
    [[UNUserNotificationCenter currentNotificationCenter]
        addNotificationRequest:request
         withCompletionHandler:^(NSError * _Nullable error) {
        KRInvoke(callback, error ? KRFailResult(1010, error.localizedDescription) : KROkResult());
    }];
}

- (void)show:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    [self addRequest:KRParams(args[KR_PARAM_KEY]) trigger:nil callback:callback];
}

- (void)scheduleAt:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSDictionary *params = KRParams(args[KR_PARAM_KEY]);
    long long timestampMs = [params[@"timestampMs"] longLongValue];
    NSDate *date = [NSDate dateWithTimeIntervalSince1970:timestampMs / 1000.0];
    if (date.timeIntervalSinceNow <= 0) {
        KRInvoke(callback, KRFailResult(1006, @"timestampMs is in the past"));
        return;
    }
    NSCalendar *calendar = [NSCalendar currentCalendar];
    NSDateComponents *components = [calendar components:(NSCalendarUnitYear | NSCalendarUnitMonth |
                                                         NSCalendarUnitDay | NSCalendarUnitHour |
                                                         NSCalendarUnitMinute | NSCalendarUnitSecond)
                                               fromDate:date];
    UNCalendarNotificationTrigger *trigger =
        [UNCalendarNotificationTrigger triggerWithDateMatchingComponents:components repeats:NO];
    [self addRequest:params trigger:trigger callback:callback];
}

- (void)showPeriodically:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSDictionary *params = KRParams(args[KR_PARAM_KEY]);
    NSTimeInterval seconds = KRIntervalSeconds(params[@"interval"] ?: @"");
    if (seconds <= 0) {
        KRInvoke(callback, KRFailResult(1007, @"unsupported interval"));
        return;
    }
    UNTimeIntervalNotificationTrigger *trigger =
        [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:seconds repeats:YES];
    [self addRequest:params trigger:trigger callback:callback];
}

#pragma mark 取消

- (void)cancel:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *identifier = KRIdentifier(KRParams(args[KR_PARAM_KEY]));
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    [center removePendingNotificationRequestsWithIdentifiers:@[ identifier ]];
    [center removeDeliveredNotificationsWithIdentifiers:@[ identifier ]];
    KRInvoke(callback, KROkResult());
}

- (void)cancelAll:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    [center removeAllPendingNotificationRequests];
    [center removeAllDeliveredNotifications];
    KRInvoke(callback, KROkResult());
}

#pragma mark 角标

- (void)setBadge:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSInteger count = [KRParams(args[KR_PARAM_KEY])[@"count"] integerValue];
    if (@available(iOS 16.0, *)) {
        [[UNUserNotificationCenter currentNotificationCenter]
            setBadgeCount:count
            withCompletionHandler:^(NSError * _Nullable error) {
            KRInvoke(callback, error ? KRFailResult(1010, error.localizedDescription) : KROkResult());
        }];
    } else {
        dispatch_async(dispatch_get_main_queue(), ^{
            [UIApplication sharedApplication].applicationIconBadgeNumber = count;
            KRInvoke(callback, KROkResult());
        });
    }
}

- (void)getBadge:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    // 注：UNUserNotificationCenter 无公开的 badge getter（仅 setBadgeCount:withCompletionHandler:），
    // 各 iOS 版本统一读取 UIApplication.applicationIconBadgeNumber。
    dispatch_async(dispatch_get_main_queue(), ^{
        NSInteger badge = [UIApplication sharedApplication].applicationIconBadgeNumber;
        KRInvoke(callback, KROkData(@{ @"count": @(badge) }));
    });
}

#pragma mark 点击事件

- (void)setNotificationClickListener:(NSDictionary *)args {
    self.clickCallback = args[KR_CALLBACK_KEY];
    [KRNotificationDelegate shared].module = self;
}

- (void)removeNotificationClickListener:(NSDictionary *)args {
    self.clickCallback = nil;
    [KRNotificationDelegate shared].module = nil;
}

- (void)handleClickEvent:(NSDictionary *)event {
    KuiklyRenderCallback callback = self.clickCallback;
    KRInvoke(callback, event);
}

#pragma mark 冷启动（同步）

/// 由 Kuikly 同步调用，返回 JSON 字符串或 nil
- (id)getLaunchNotification:(NSDictionary *)args {
    NSDictionary *payload = [[KRNotificationDelegate shared] consumeLaunchPayload];
    if (!payload) { return nil; }
    NSData *data = [NSJSONSerialization dataWithJSONObject:payload options:0 error:nil];
    if (!data) { return nil; }
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

@end
