package io.github.arialentropy.notification.android

import android.content.Intent
import org.json.JSONObject

/** 通知数据（Module 与 Receiver 共用） */
internal data class KRNotificationPayload(
    val id: Int,
    val title: String,
    val body: String,
    val channelId: String,
    val payload: String?,
    val sound: String?,
    val groupKey: String?,
    val showWhenInForeground: Boolean
) {

    fun writeTo(intent: Intent): Intent = intent.apply {
        putExtra(KEY_ID, id)
        putExtra(KEY_TITLE, title)
        putExtra(KEY_BODY, body)
        putExtra(KEY_CHANNEL, channelId)
        putExtra(KEY_PAYLOAD, payload)
        putExtra(KEY_SOUND, sound)
        putExtra(KEY_GROUP, groupKey)
        putExtra(KEY_FOREGROUND, showWhenInForeground)
    }

    companion object {
        const val KEY_ID = "kr_id"
        const val KEY_TITLE = "kr_title"
        const val KEY_BODY = "kr_body"
        const val KEY_CHANNEL = "kr_channel"
        const val KEY_PAYLOAD = "kr_payload"
        const val KEY_SOUND = "kr_sound"
        const val KEY_GROUP = "kr_group"
        const val KEY_FOREGROUND = "kr_foreground"
        const val KEY_ACTION = "kr_action"

        fun fromJson(json: JSONObject): KRNotificationPayload = KRNotificationPayload(
            id = json.optInt("id"),
            title = json.optString("title"),
            body = json.optString("body"),
            channelId = json.optString("channelId", "default").ifEmpty { "default" },
            payload = json.optString("payload").takeIf { it.isNotEmpty() },
            sound = json.optString("sound").takeIf { it.isNotEmpty() },
            groupKey = json.optString("groupKey").takeIf { it.isNotEmpty() },
            showWhenInForeground = json.optBoolean("showWhenInForeground", false)
        )

        fun fromIntent(intent: Intent): KRNotificationPayload = KRNotificationPayload(
            id = intent.getIntExtra(KEY_ID, -1),
            title = intent.getStringExtra(KEY_TITLE) ?: "",
            body = intent.getStringExtra(KEY_BODY) ?: "",
            channelId = intent.getStringExtra(KEY_CHANNEL) ?: "default",
            payload = intent.getStringExtra(KEY_PAYLOAD),
            sound = intent.getStringExtra(KEY_SOUND),
            groupKey = intent.getStringExtra(KEY_GROUP),
            showWhenInForeground = intent.getBooleanExtra(KEY_FOREGROUND, false)
        )
    }
}
