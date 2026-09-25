Pod::Spec.new do |s|
  s.name             = 'KuiklyNotificationIOS'
  s.version          = '1.0.0'
  s.summary          = 'KuiklyNotification iOS native implementation'
  s.description      = <<-DESC
    iOS native local notification module for the Kuikly cross-platform framework.
  DESC
  s.homepage         = 'https://github.com/AriaLEntropy/KuiklyNotification'
  s.license          = { :type => 'Apache-2.0', :file => 'LICENSE' }
  s.author           = { 'AriaLEntropy' => 'AriaLEntropy@users.noreply.github.com' }
  s.source           = { :git => 'https://github.com/AriaLEntropy/KuiklyNotification.git', :tag => s.version.to_s }

  s.ios.deployment_target = '12.0'
  s.source_files     = 'KuiklyNotificationIOS/Classes/**/*.{h,m}'
  s.frameworks       = 'UserNotifications', 'UIKit'

  # Kuikly iOS 渲染库（版本随宿主 Kuikly 版本，如 '~> 2.24.0'）
  s.dependency 'OpenKuiklyIOSRender'
end
