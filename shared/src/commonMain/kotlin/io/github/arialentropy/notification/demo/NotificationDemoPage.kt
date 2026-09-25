package io.github.arialentropy.notification.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*
import io.github.arialentropy.notification.const.NotificationConst
import io.github.arialentropy.notification.model.NotificationClickEvent
import io.github.arialentropy.notification.model.NotificationRequest
import io.github.arialentropy.notification.module.KRNotificationModule

/** 演示用按钮（Kuikly 核心无 Button 组件，用 View + Text + click 组合） */
private fun ViewContainer<*, *>.demoButton(title: String, onClick: () -> Unit) {
    View {
        attr {
            height(44f)
            backgroundColor(Color(0xFFEEEEEE))
            alignItems(FlexAlign.CENTER)
            marginBottom(8f)
        }
        event {
            click { onClick() }
        }
        Text {
            attr { text(title) }
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
        return {
            View {
                attr {
                    flexDirection(FlexDirection.COLUMN)
                    padding(16f)
                }

                Text {
                    attr {
                        text("KuiklyNotification Demo")
                        fontSize(18f)
                        marginBottom(12f)
                    }
                }

                demoButton("1. 请求权限") {
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

                Text {
                    attr {
                        text(ctx.logText)
                        fontSize(12f)
                        color(Color(0xFF666666L))
                        marginTop(16f)
                    }
                }
            }
        }
    }
}
