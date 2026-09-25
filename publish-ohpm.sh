#!/usr/bin/env bash
#
# 发布鸿蒙 HAR 到 ohpm 中心仓（https://ohpm.openharmony.cn）。
#
# 前置（一次性，需账号）：
#   1) 注册 OpenHarmony 三方库中心仓账号
#      注意：带作用域（@group/包名）要求该作用域是「已认证的组织」，个人账号会报
#      400 "Failed to verify the OHPM package group"；故本包使用**不带作用域**的名字
#   2) 在个人/组织中心生成 publish_id 与 SSH 密钥对：公钥上传，私钥存本地
#   3) 配置 ~/.ohpm/.ohpmrc（或改用命令行参数）：
#        publish_registry=https://ohpm.openharmony.cn/ohpm/
#        publish_id=<your-publish-id>
#        key_path=<path-to-private-key>
#
# 未注册账号时，本脚本的 prepublish 仍然可用（只做本地预校验，不上传）。
#
# Windows 等价命令（PowerShell）：
#   cd ohosApp; hvigorw assembleHar --no-daemon; cd ..
#   ohpm prepublish KuiklyNotificationOhos/build/default/outputs/default/KuiklyNotificationOhos.har
#   ohpm publish    KuiklyNotificationOhos/build/default/outputs/default/KuiklyNotificationOhos.har

set -euo pipefail

cd "$(dirname "$0")"

HAR="KuiklyNotificationOhos/build/default/outputs/default/KuiklyNotificationOhos.har"

echo "[1/3] 构建 HAR ..."
(cd ohosApp && hvigorw assembleHar --no-daemon)

echo "[2/3] 预校验 ..."
ohpm prepublish "$HAR"

echo "[3/3] 发布 ..."
ohpm publish "$HAR"

echo ""
echo "已发布 kuikly-notification-ohos"
