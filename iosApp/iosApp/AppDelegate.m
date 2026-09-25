#import "AppDelegate.h"
#import "KuiklyRenderViewController.h"

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    // Demo 页面：shared 模块中 @Page("router")
    KuiklyRenderViewController *vc =
        [[KuiklyRenderViewController alloc] initWithPageName:@"router" pageData:nil];
    UINavigationController *nav = [[UINavigationController alloc] initWithRootViewController:vc];
    self.window.rootViewController = nav;
    [self.window makeKeyAndVisible];
    return YES;
}

@end
