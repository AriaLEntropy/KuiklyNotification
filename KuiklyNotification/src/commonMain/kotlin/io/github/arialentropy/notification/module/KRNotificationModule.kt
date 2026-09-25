package io.github.arialentropy.notification.module

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import io.github.arialentropy.notification.const.NotificationConst
import io.github.arialentropy.notification.ext.JsonResult
import io.github.arialentropy.notification.ext.JsonResultCallback
import io.github.arialentropy.notification.model.NotificationClickEvent
import io.github.arialentropy.notification.model.NotificationConfig
import io.github.arialentropy.notification.model.NotificationRequest

/**
 * 跨端本地通知模块（Kuikly 侧）。
 *
 * 使用：
 * ```
 * val module = acquireModule<KRNotificationModule>(KRNotificationModule.MODULE_NAME)
 * module.configure(NotificationConfig(hostActivity = "...", smallIconResId = R.drawable.ic)) { }
 * module.show(NotificationRequest(id = 1, title = "标题", body = "内容")) { }
 * ```
 */
class KRNotificationModule : KRNotificationBaseModule() {

    private var clickListener: ((NotificationClickEvent) -> Unit)? = null

    override fun moduleName(): String = MODULE_NAME

    // ---------------- 初始化 ----------------

    /** 注入宿主配置（Android/鸿蒙必需；iOS 无需调用） */
    fun configure(config: NotificationConfig, cb: JsonResultCallback) =
        post("configure", config.toJson(), cb)

    // ---------------- 权限 ----------------

    fun requestPermission(cb: JsonResultCallback) = post("requestPermission", null, cb)

    fun checkPermission(cb: JsonResultCallback) = post("checkPermission", null, cb)

    // ---------------- 渠道 ----------------

    fun createChannel(
        channelId: String,
        name: String,
        importance: String = NotificationConst.Importance.DEFAULT,
        cb: JsonResultCallback
    ) = post(
        "createChannel",
        JSONObject().apply {
            put("channelId", channelId)
            put("name", name)
            put("importance", importance)
        },
        cb
    )

    // ---------------- 发送 / 取消 ----------------

    fun show(request: NotificationRequest, cb: JsonResultCallback) =
        post("show", request.toJson(), cb)

    fun scheduleAt(request: NotificationRequest, timestampMs: Long, cb: JsonResultCallback) =
        post("scheduleAt", request.toJson().apply { put("timestampMs", timestampMs) }, cb)

    fun showPeriodically(request: NotificationRequest, interval: String, cb: JsonResultCallback) =
        post("showPeriodically", request.toJson().apply { put("interval", interval) }, cb)

    fun cancel(id: Int, cb: JsonResultCallback) =
        post("cancel", JSONObject().apply { put("id", id) }, cb)

    fun cancelAll(cb: JsonResultCallback) = post("cancelAll", null, cb)

    // ---------------- 角标（iOS） ----------------

    fun setBadge(count: Int, cb: JsonResultCallback) =
        post("setBadge", JSONObject().apply { put("count", count) }, cb)

    fun getBadge(cb: JsonResultCallback) = post("getBadge", null, cb)

    // ---------------- 厂商适配 / 设置引导（Android；其他端回调 UNSUPPORTED） ----------------

    /** 电池优化是否开启（true=未加入白名单）。data: {enabled: Boolean} */
    fun isBatteryOptimizationEnabled(cb: JsonResultCallback) =
        post("isBatteryOptimizationEnabled", null, cb)

    /** 打开系统「电池优化」设置页（引导用户加入白名单） */
    fun openBatteryOptimizationSettings(cb: JsonResultCallback) =
        post("openBatteryOptimizationSettings", null, cb)

    /** 打开厂商「自启动 / 后台管理」设置页（小米/华为/荣耀/OPPO/vivo/魅族，失败回退应用详情） */
    fun openAutoStartSettings(cb: JsonResultCallback) =
        post("openAutoStartSettings", null, cb)

    /** 打开本应用的系统通知设置页 */
    fun openNotificationSettings(cb: JsonResultCallback) =
        post("openNotificationSettings", null, cb)

    // ---------------- 点击事件（常驻） ----------------

    /** 注册点击监听；后注册覆盖前一个 */
    fun setNotificationClickListener(listener: (NotificationClickEvent) -> Unit) {
        clickListener = listener
        callNativeKeepAlive("setNotificationClickListener", null) { result ->
            NotificationClickEvent.from(result)?.let { clickListener?.invoke(it) }
        }
    }

    fun removeNotificationClickListener() {
        clickListener = null
        post("removeNotificationClickListener", null, null)
    }

    // ---------------- 冷启动（同步） ----------------

    /** 获取冷启动拉起通知，取一次后不再返回 */
    fun getLaunchNotification(): NotificationClickEvent? =
        NotificationClickEvent.from(syncCallNativeJson("getLaunchNotification", null))

    // ---------------- 内部 ----------------

    private fun post(method: String, data: JSONObject?, cb: JsonResultCallback?) {
        callNative(method, data) { result -> cb?.invoke(JsonResult(result)) }
    }

    companion object {
        const val MODULE_NAME = NotificationConst.MODULE_NAME
    }
}
