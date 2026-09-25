package io.github.arialentropy.notification.module

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject

/**
 * 跨端本地通知模块（Android 实现）。
 *
 * 注册：宿主在 `registerExternalModule` 中
 * ```
 * moduleExport("KRNotificationModule") { KRNotificationModule() }
 * ```
 * 初始化：`configure` 注入入口 Activity 与小图标。
 */
class KRNotificationModule : KuiklyRenderBaseModule() {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val ctx: Context get() = requireNotNull(context) { "Kuikly context is unavailable" }

    private val store: KRNotificationStore get() = KRNotificationStore(ctx)

    private val notificationManager: NotificationManager
        get() = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val alarmManager: AlarmManager
        get() = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private var clickCallback: KuiklyRenderCallback? = null

    // ---------------- Module 入口 ----------------

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? =
        dispatch(method, params, callback)

    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? =
        dispatch(method, params?.toString(), callback)

    override fun onDestroy() {
        super.onDestroy()
        clickCallback = null
        KRNotificationLaunch.detach(this)
    }

    /** 由宿主入口 Activity 的 intent 或 App 存活时的点击触发 */
    internal fun onNotificationClick(id: Int, payload: String?, action: String) {
        clickCallback?.invoke(
            hashMapOf<String, Any?>("id" to id, "payload" to payload, "action" to action)
        )
    }

    private fun dispatch(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        KRNotificationLaunch.attach(this)
        return when (method) {
            "configure" -> { configure(params, callback); null }
            "requestPermission" -> { requestPermission(callback); null }
            "checkPermission" -> { checkPermission(callback); null }
            "createChannel" -> { createChannel(params, callback); null }
            "show" -> { show(params, callback); null }
            "scheduleAt" -> { scheduleAt(params, callback); null }
            "showPeriodically" -> { showPeriodically(params, callback); null }
            "cancel" -> { cancel(params, callback); null }
            "cancelAll" -> { cancelAll(callback); null }
            "setBadge" -> { unsupported(callback); null }
            "getBadge" -> { unsupported(callback); null }
            "setNotificationClickListener" -> { clickCallback = callback; null }
            "removeNotificationClickListener" -> { clickCallback = null; null }
            "getLaunchNotification" -> getLaunchNotification()
            else -> {
                callback?.invoke(failResult(ERROR_INVALID_REQUEST, "unknown method: $method"))
                null
            }
        }
    }

    // ---------------- 初始化 ----------------

    private fun configure(params: String?, callback: KuiklyRenderCallback?) {
        val json = parse(params) ?: run {
            callback?.invoke(failResult(ERROR_INVALID_REQUEST, "invalid params"))
            return
        }
        json.optString("hostActivity").takeIf { it.isNotEmpty() }?.let { store.hostActivity = it }
        if (json.has("smallIconResId")) store.smallIconResId = json.optInt("smallIconResId")
        callback?.invoke(okResult())
    }

    // ---------------- 权限 ----------------

    private fun requestPermission(callback: KuiklyRenderCallback?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            callback?.invoke(okResult(mapOf("status" to STATUS_GRANTED)))
            return
        }
        mainHandler.post {
            val host = this.activity
            if (host is FragmentActivity) {
                KRNotificationPermissionFragment.request(host) { granted ->
                    store.hasRequestedPermission = true
                    callback?.invoke(
                        okResult(mapOf("status" to if (granted) STATUS_GRANTED else STATUS_DENIED))
                    )
                }
            } else {
                store.hasRequestedPermission = true
                callback?.invoke(failResult(ERROR_INVALID_CONFIG, "host activity is not FragmentActivity"))
            }
        }
    }

    private fun checkPermission(callback: KuiklyRenderCallback?) {
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            val enabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            when {
                granted && enabled -> STATUS_GRANTED
                !store.hasRequestedPermission -> STATUS_NOT_DETERMINED
                else -> STATUS_DENIED
            }
        } else {
            if (NotificationManagerCompat.from(ctx).areNotificationsEnabled()) STATUS_GRANTED
            else STATUS_DENIED
        }
        callback?.invoke(okResult(mapOf("status" to status)))
    }

    // ---------------- 渠道 ----------------

    private fun createChannel(params: String?, callback: KuiklyRenderCallback?) {
        val json = parse(params) ?: run {
            callback?.invoke(failResult(ERROR_INVALID_REQUEST, "invalid params"))
            return
        }
        val channelId = json.optString("channelId").ifEmpty { KRNotificationPublisher.CHANNEL_DEFAULT }
        val name = json.optString("name").ifEmpty { channelId }
        val importance = json.optString("importance", "DEFAULT")
        KRNotificationPublisher.createChannel(notificationManager, channelId, name, importance)
        callback?.invoke(okResult())
    }

    // ---------------- 发送 ----------------

    private fun show(params: String?, callback: KuiklyRenderCallback?) {
        val data = parsePayload(params, callback) ?: return
        if (!isConfigured()) {
            callback?.invoke(failResult(ERROR_INVALID_CONFIG, "configure() not called"))
            return
        }
        if (!hasPermission()) {
            callback?.invoke(failResult(ERROR_PERMISSION_DENIED, "notification permission denied"))
            return
        }
        val ok = KRNotificationPublisher.publish(ctx, data)
        callback?.invoke(if (ok) okResult() else failResult(ERROR_INTERNAL, "publish failed"))
    }

    private fun scheduleAt(params: String?, callback: KuiklyRenderCallback?) {
        val json = parse(params) ?: run {
            callback?.invoke(failResult(ERROR_INVALID_REQUEST, "invalid params"))
            return
        }
        val data = KRNotificationPayload.fromJson(json)
        if (!isConfigured()) {
            callback?.invoke(failResult(ERROR_INVALID_CONFIG, "configure() not called"))
            return
        }
        val timestampMs = json.optLong("timestampMs")
        val pi = buildSchedulePendingIntent(data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestampMs, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestampMs, pi)
        }
        store.addScheduledId(data.id)
        callback?.invoke(okResult())
    }

    private fun showPeriodically(params: String?, callback: KuiklyRenderCallback?) {
        val json = parse(params) ?: run {
            callback?.invoke(failResult(ERROR_INVALID_REQUEST, "invalid params"))
            return
        }
        val data = KRNotificationPayload.fromJson(json)
        if (!isConfigured()) {
            callback?.invoke(failResult(ERROR_INVALID_CONFIG, "configure() not called"))
            return
        }
        val interval = json.optString("interval", "DAY")
        val millis = intervalToMillis(interval)
        if (millis <= 0L) {
            callback?.invoke(failResult(ERROR_UNSUPPORTED_INTERVAL, "unsupported interval: $interval"))
            return
        }
        val pi = buildSchedulePendingIntent(data)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, millis, pi)
        store.addScheduledId(data.id)
        callback?.invoke(okResult())
    }

    // ---------------- 取消 ----------------

    private fun cancel(params: String?, callback: KuiklyRenderCallback?) {
        val json = parse(params) ?: run {
            callback?.invoke(failResult(ERROR_INVALID_REQUEST, "invalid params"))
            return
        }
        val id = json.optInt("id")
        notificationManager.cancel(id)
        alarmManager.cancel(buildSchedulePendingIntent(id))
        store.removeScheduledId(id)
        callback?.invoke(okResult())
    }

    private fun cancelAll(callback: KuiklyRenderCallback?) {
        notificationManager.cancelAll()
        store.scheduledIds().forEach { id -> alarmManager.cancel(buildSchedulePendingIntent(id)) }
        store.clearScheduledIds()
        callback?.invoke(okResult())
    }

    // ---------------- 冷启动 ----------------

    /** 同步返回：`{"id","payload","action"}` 或 null */
    private fun getLaunchNotification(): Any? {
        val launch = store.consumeLaunch() ?: return null
        return JSONObject().apply {
            put("id", launch.first)
            put("payload", launch.second)
            put("action", launch.third)
        }.toString()
    }

    // ---------------- 内部工具 ----------------

    private fun isConfigured(): Boolean =
        !store.hostActivity.isNullOrEmpty() && store.smallIconResId != 0

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildSchedulePendingIntent(data: KRNotificationPayload): PendingIntent {
        val intent = Intent(ctx, KRNotificationReceiver::class.java).setAction(ACTION_SCHEDULE)
        data.writeTo(intent)
        return PendingIntent.getBroadcast(
            ctx, data.id, intent, KRNotificationPublisher.pendingFlags()
        )
    }

    /** requestCode=id + 固定 action，可重建出同一个 PendingIntent 用于取消 */
    private fun buildSchedulePendingIntent(id: Int): PendingIntent {
        val intent = Intent(ctx, KRNotificationReceiver::class.java).setAction(ACTION_SCHEDULE)
        return PendingIntent.getBroadcast(
            ctx, id, intent, KRNotificationPublisher.pendingFlags()
        )
    }

    private fun parse(params: String?): JSONObject? =
        runCatching { JSONObject(params ?: "{}") }.getOrNull()

    private fun parsePayload(params: String?, callback: KuiklyRenderCallback?): KRNotificationPayload? {
        val json = parse(params)
        if (json == null) {
            callback?.invoke(failResult(ERROR_INVALID_REQUEST, "invalid params"))
            return null
        }
        if (!json.has("id")) {
            callback?.invoke(failResult(ERROR_INVALID_REQUEST, "missing id"))
            return null
        }
        return KRNotificationPayload.fromJson(json)
    }

    private fun intervalToMillis(interval: String): Long = when (interval) {
        "MINUTE" -> 60_000L
        "HOUR" -> 3_600_000L
        "HALF_DAY" -> 43_200_000L
        "DAY" -> 86_400_000L
        "WEEK" -> 604_800_000L
        else -> -1L
    }

    private fun okResult(data: Map<String, Any?>? = null): Map<String, Any?> =
        mapOf("code" to 0, "msg" to "", "data" to (data ?: emptyMap<String, Any?>()))

    private fun failResult(code: Int, msg: String): Map<String, Any?> =
        mapOf("code" to code, "msg" to msg, "data" to null)

    private fun unsupported(callback: KuiklyRenderCallback?) {
        callback?.invoke(failResult(ERROR_UNSUPPORTED, "unsupported on Android"))
    }

    companion object {
        const val MODULE_NAME = "KRNotificationModule"

        private const val ACTION_SCHEDULE = "io.github.arialentropy.notification.SCHEDULE"

        private const val STATUS_GRANTED = "GRANTED"
        private const val STATUS_DENIED = "DENIED"
        private const val STATUS_NOT_DETERMINED = "NOT_DETERMINED"

        private const val ERROR_INVALID_CONFIG = 1001
        private const val ERROR_PERMISSION_DENIED = 1002
        private const val ERROR_UNSUPPORTED_INTERVAL = 1007
        private const val ERROR_UNSUPPORTED = 1010
        private const val ERROR_INVALID_REQUEST = 1011
        private const val ERROR_INTERNAL = -1
    }
}
