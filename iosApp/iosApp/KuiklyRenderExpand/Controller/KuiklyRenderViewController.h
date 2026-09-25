#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/// Kuikly 页面容器（对齐官方 KuiklyRenderViewController）
@interface KuiklyRenderViewController : UIViewController

/*
 * @brief 创建实例对应的初始化方法.
 * @param pageName 页面名（对应 kotlin 侧 @Page("xxx") 中的 xxx）
 * @param pageData 页面对应的参数（kotlin 侧可通过 pageData.params 获取）
 */
- (instancetype)initWithPageName:(NSString *)pageName pageData:(NSDictionary *_Nullable)pageData;

@end

NS_ASSUME_NONNULL_END
