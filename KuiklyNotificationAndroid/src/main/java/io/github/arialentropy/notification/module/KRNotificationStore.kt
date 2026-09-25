package io.github.arialentropy.notification.module

import android.content.Context

/**
 * 本地持久化：
 * - 宿主配置（入口 Activity、小图标）
 * - 是否已请求过权限（用于推导 NOT_DETERMINED）
 * - 冷启动拉起 payload（消费一次）
 * - 已排期的通知 id（用于 cancelAll）
 */
internal class KRNotificationStore(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    var hostActivity: String?
        get() = sp.getString(KEY_HOST_ACTIVITY, null)
        set(value) = sp.edit().putString(KEY_HOST_ACTIVITY, value).apply()

    var smallIconResId: Int
        get() = sp.getInt(KEY_SMALL_ICON, 0)
        set(value) = sp.edit().putInt(KEY_SMALL_ICON, value).apply()

    var hasRequestedPermission: Boolean
        get() = sp.getBoolean(KEY_REQUESTED, false)
        set(value) = sp.edit().putBoolean(KEY_REQUESTED, value).apply()

    // ---------------- 冷启动 ----------------

    fun saveLaunch(id: Int, payload: String?, action: String) {
        sp.edit()
            .putBoolean(KEY_HAS_LAUNCH, true)
            .putInt(KEY_LAUNCH_ID, id)
            .putString(KEY_LAUNCH_PAYLOAD, payload)
            .putString(KEY_LAUNCH_ACTION, action)
            .apply()
    }

    /** 取一次后清除 */
    fun consumeLaunch(): Triple<Int, String?, String>? {
        if (!sp.getBoolean(KEY_HAS_LAUNCH, false)) return null
        val result = Triple(
            sp.getInt(KEY_LAUNCH_ID, -1),
            sp.getString(KEY_LAUNCH_PAYLOAD, null),
            sp.getString(KEY_LAUNCH_ACTION, "default") ?: "default"
        )
        sp.edit().putBoolean(KEY_HAS_LAUNCH, false).apply()
        return result
    }

    // ---------------- 已排期 id ----------------

    fun addScheduledId(id: Int) {
        val ids = scheduledIds().toMutableSet().apply { add(id) }
        sp.edit().putStringSet(KEY_SCHEDULED, ids.map { it.toString() }.toSet()).apply()
    }

    fun removeScheduledId(id: Int) {
        val ids = scheduledIds().toMutableSet().apply { remove(id) }
        sp.edit().putStringSet(KEY_SCHEDULED, ids.map { it.toString() }.toSet()).apply()
    }

    fun clearScheduledIds() {
        sp.edit().remove(KEY_SCHEDULED).apply()
    }

    fun scheduledIds(): Set<Int> =
        sp.getStringSet(KEY_SCHEDULED, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

    companion object {
        private const val SP_NAME = "kr_notification"
        private const val KEY_HOST_ACTIVITY = "host_activity"
        private const val KEY_SMALL_ICON = "small_icon"
        private const val KEY_REQUESTED = "requested"
        private const val KEY_HAS_LAUNCH = "has_launch"
        private const val KEY_LAUNCH_ID = "launch_id"
        private const val KEY_LAUNCH_PAYLOAD = "launch_payload"
        private const val KEY_LAUNCH_ACTION = "launch_action"
        private const val KEY_SCHEDULED = "scheduled_ids"
    }
}
