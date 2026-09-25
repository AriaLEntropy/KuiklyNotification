package io.github.arialentropy.notification.model

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/** 通知请求 */
data class NotificationRequest(
    /** 整数唯一标识，重复 id 会覆盖 */
    val id: Int,
    val title: String,
    val body: String,
    /** 渠道 id（Android / 鸿蒙） */
    val channelId: String = "default",
    /** 点击时原样返回的业务数据，建议 ≤1KB */
    val payload: String? = null,
    /** App 在前台时是否仍展示，默认 false */
    val showWhenInForeground: Boolean = false,
    /** iOS 角标数 */
    val badge: Int? = null,
    /** 声音资源名：Android raw 名 / iOS 带后缀文件名 / 鸿蒙 rawfile 名 */
    val sound: String? = null,
    /** 分组聚合键（Android group / iOS threadIdentifier） */
    val groupKey: String? = null
) {

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("body", body)
        put("channelId", channelId)
        put("showWhenInForeground", showWhenInForeground)
        payload?.let { put("payload", it) }
        badge?.let { put("badge", it) }
        sound?.let { put("sound", it) }
        groupKey?.let { put("groupKey", it) }
    }
}
