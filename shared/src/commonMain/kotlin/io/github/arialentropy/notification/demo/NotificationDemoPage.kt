package io.github.arialentropy.notification.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import io.github.arialentropy.notification.const.NotificationConst
import io.github.arialentropy.notification.model.NotificationClickEvent
import io.github.arialentropy.notification.model.NotificationRequest
import io.github.arialentropy.notification.module.KRNotificationModule

private val PAGE_BG = Color(0xFFF2F3F5)
private val CARD_BG = Color(0xFFFFFFFF)
private val TEXT_MAIN = Color(0xFF1F2329)
private val TEXT_SUB = Color(0xFF8A9099)
private val LOG_BG = Color(0xFF1E1E1E)
private val LOG_TEXT = Color(0xFF9CDCFE)

/** 分组标题 */
private fun ViewContainer<*, *>.sectionLabel(title: String) {
    Text {
        attr {
            text(title)
            fontSize(12f)
            color(TEXT_SUB)
            marginTop(4f)
            marginBottom(8f)
        }
    }
}

/** 演示用按钮（Kuikly 核心无 Button 组件，用 View + Text + click 组合） */
private fun ViewContainer<*, *>.demoButton(title: String, onClick: () -> Unit) {
    View {
        attr {
            height(46f)
            backgroundColor(CARD_BG)
            borderRadius(10f)
            allCenter()
            marginBottom(8f)
        }
        event {
            click { onClick() }
        }
        Text {
            attr {
                text(title)
                fontSize(13f)
                color(TEXT_MAIN)
            }
        }
    }
}

/** 本地通知能力演示页面 */
@Page("router", supportInLocal = true)
internal class NotificationDemoPage : BasePager() {

    private var logText by observable("")

    private val notification: KRNotificationModule
        get() = acquireModule<KRNotificationModule>(KRNotificationModule.MODULE_NAME)

    override fun created() {
        super.created()
        notification.getLaunchNotification()?.let { event ->
            appendLog("冷启动拉起: id=${event.id}, payload=${event.payload}")
        }
        notification.setNotificationClickListener { event: NotificationClickEvent ->
            appendLog("点击通知: id=${event.id}, payload=${event.payload}")
        }
    }

    private fun appendLog(line: String) {
        logText = "$line\n$logText"
    }

    override fun body(): ViewBuilder {
        val ctx = this
        // 安全区：iOS 刘海/状态栏、底部 Home 指示器，避免内容被遮挡
        val safe = pagerData.safeAreaInsets
        return {
            View {
                attr {
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    flexDirection(FlexDirection.COLUMN)
                    backgroundColor(PAGE_BG)
                    padding(
                        top = safe.top + 12f,
                        left = 16f,
                        bottom = safe.bottom + 12f,
                        right = 16f
                    )
                }

                // 头部卡片
                View {
                    attr {
                        backgroundColor(CARD_BG)
                        borderRadius(12f)
                        padding(14f)
                        marginBottom(12f)
                        flexDirection(FlexDirection.COLUMN)
                    }
                    Text {
                        attr {
                            text("KuiklyNotification Demo")
                            fontSize(17f)
                            fontWeightBold()
                            color(TEXT_MAIN)
                        }
                    }
                    Text {
                        attr {
                            text("本地通知能力演示（Android / iOS / 鸿蒙）")
                            fontSize(11f)
                            color(TEXT_SUB)
                            marginTop(4f)
                        }
                    }
                }

                Scroller {
                    attr {
                        flex(1f)
                        flexDirection(FlexDirection.COLUMN)
                    }

                    sectionLabel("操作")

                    demoButton("1. 请求权限（Android13+ 才弹窗）") {
                        ctx.notification.requestPermission { result ->
                            ctx.appendLog("权限: code=${result.code}, ${result.data?.optString("status")}")
                        }
                    }

                    demoButton("2. 立即发通知") {
                        val request = NotificationRequest(
                            id = 1,
                            title = "标题",
                            body = "这是一条本地通知",
                            payload = "demo_payload"
                        )
                        ctx.notification.show(request) { result ->
                            ctx.appendLog("show: code=${result.code}")
                        }
                    }

                    demoButton("3. 10 秒后定时通知") {
                        val request = NotificationRequest(
                            id = 2,
                            title = "定时通知",
                            body = "10 秒后触发",
                            payload = "schedule_payload"
                        )
                        val triggerAt = DateTime.currentTimestamp() + 10_000L
                        ctx.notification.scheduleAt(request, triggerAt) { result ->
                            ctx.appendLog("scheduleAt: code=${result.code}")
                        }
                    }

                    demoButton("4. 每天重复通知") {
                        val request = NotificationRequest(
                            id = 3,
                            title = "每日通知",
                            body = "每天重复",
                            payload = "periodic_payload"
                        )
                        ctx.notification.showPeriodically(request, NotificationConst.Interval.DAY) { result ->
                            ctx.appendLog("showPeriodically: code=${result.code}")
                        }
                    }

                    demoButton("5. 取消 id=1") {
                        ctx.notification.cancel(1) { result ->
                            ctx.appendLog("cancel: code=${result.code}")
                        }
                    }

                    demoButton("6. 取消全部") {
                        ctx.notification.cancelAll { result ->
                            ctx.appendLog("cancelAll: code=${result.code}")
                        }
                    }

                    demoButton("7. 设置角标 5（iOS）") {
                        ctx.notification.setBadge(5) { result ->
                            ctx.appendLog("setBadge: code=${result.code}")
                        }
                    }

                    demoButton("8. 创建高优先级渠道 high") {
                        ctx.notification.createChannel("high", "高优先级", NotificationConst.Importance.HIGH) { result ->
                            ctx.appendLog("createChannel: code=${result.code}")
                        }
                    }

                    demoButton("9. 3 秒后横幅通知（high 渠道）") {
                        val request = NotificationRequest(
                            id = 10,
                            title = "横幅通知",
                            body = "IMPORTANCE_HIGH 才会出横幅",
                            channelId = "high",
                            payload = "banner_payload",
                            showWhenInForeground = true
                        )
                        val triggerAt = DateTime.currentTimestamp() + 3_000L
                        ctx.notification.scheduleAt(request, triggerAt) { result ->
                            ctx.appendLog("banner schedule: code=${result.code}")
                        }
                    }

                    demoButton("10. 电池优化是否开启") {
                        ctx.notification.isBatteryOptimizationEnabled { result ->
                            ctx.appendLog("batteryOptimization: code=${result.code}, ${result.data}")
                        }
                    }

                    demoButton("11. 打开电池优化设置") {
                        ctx.notification.openBatteryOptimizationSettings { result ->
                            ctx.appendLog("openBatteryOptimizationSettings: code=${result.code}")
                        }
                    }

                    demoButton("12. 打开自启动设置") {
                        ctx.notification.openAutoStartSettings { result ->
                            ctx.appendLog("openAutoStartSettings: code=${result.code}")
                        }
                    }

                    sectionLabel("日志")

                    View {
                        attr {
                            backgroundColor(LOG_BG)
                            borderRadius(10f)
                            padding(10f)
                            marginBottom(8f)
                            flexDirection(FlexDirection.COLUMN)
                        }
                        Text {
                            attr {
                                text(ctx.logText.ifEmpty { "（操作结果会显示在这里）" })
                                fontSize(11f)
                                color(LOG_TEXT)
                                lineHeight(16f)
                            }
                        }
                    }
                }
            }
        }
    }
}
