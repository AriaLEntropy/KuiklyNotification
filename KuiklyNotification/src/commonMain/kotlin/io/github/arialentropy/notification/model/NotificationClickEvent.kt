package io.github.arialentropy.notification.model

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/** 通知点击事件（常驻监听 / 冷启动拉起） */
data class NotificationClickEvent(
    val id: Int,
    val payload: String?,
    /** 预留：动作类型 */
    val action: String = "default"
) {

    companion object {

        /** 从事件 JSON 解析；null 返回 null */
        fun from(json: JSONObject?): NotificationClickEvent? {
            if (json == null) return null
            return NotificationClickEvent(
                id = json.optInt("id"),
                payload = json.optString("payload").takeIf { it.isNotEmpty() },
                action = json.optString("action", "default")
            )
        }
    }
}
