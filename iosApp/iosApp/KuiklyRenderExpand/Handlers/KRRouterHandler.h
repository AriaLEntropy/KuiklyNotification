#import <Foundation/Foundation.h>
#import <OpenKuiklyIOSRender/KRRouterModule.h>

NS_ASSUME_NONNULL_BEGIN

/// Kuikly 页面路由适配器：把 Kuikly 内的 openPage/closePage 映射到原生导航
@interface KRRouterHandler : NSObject <KRRouterProtocol>

@end

NS_ASSUME_NONNULL_END
