#!/usr/bin/env bash
#
# 发布 KMP + Android 构件到仓库内的 maven-repo/ 目录。
#
# 之后把 maven-repo/ 推到 gh-pages 分支，它就成为一个公开 Maven 仓库：
#   - GitHub Pages: https://<user>.github.io/KuiklyNotification/maven-repo/
#   - 或 raw 直连: https://raw.githubusercontent.com/<user>/KuiklyNotification/gh-pages/
# 消费者无需任何账号即可拉取（详见 README「引入依赖」）。
#
# iOS 不走 Maven：由 CocoaPods 直接引用 git tag（pod ..., :git => ..., :tag => ...）。
# 鸿蒙不走 Maven：ohpm 依赖本地/发布的 HAR（详见 README 鸿蒙接入）。
#
# Windows 等价命令（PowerShell）：
#   .\gradlew.bat :KuiklyNotification:publishAllPublicationsToLocalRepoRepository `
#                 :KuiklyNotificationAndroid:publishReleasePublicationToLocalRepoRepository

set -euo pipefail

cd "$(dirname "$0")"

./gradlew \
  :KuiklyNotification:publishAllPublicationsToLocalRepoRepository \
  :KuiklyNotificationAndroid:publishReleasePublicationToLocalRepoRepository

echo ""
echo "已发布到 maven-repo/"
echo "下一步（把仓库内容推到 gh-pages 分支）："
echo "  git add maven-repo && git commit -m 'chore(release): publish maven artifacts'"
echo "  git subtree push --prefix maven-repo origin gh-pages"
