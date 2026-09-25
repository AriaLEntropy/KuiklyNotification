package io.github.arialentropy.notification.module

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** 构建并展示通知 */
internal object KRNotificationPublisher {

    const val CHANNEL_DEFAULT = "default"

    /** @return true 表示已展示（或被前台策略跳过），false 表示配置缺失 */
    fun publish(context: Context, data: KRNotificationPayload): Boolean {
        val store = KRNotificationStore(context)
        val smallIcon = store.smallIconResId
        val hostActivity = store.hostActivity
        if (smallIcon == 0 || hostActivity.isNullOrEmpty()) return false

        // 前台且不允许前台展示时跳过
        KRAppForeground.ensureRegistered(context)
        if (!data.showWhenInForeground && KRAppForeground.isForeground) return true

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = resolveChannel(manager, data.channelId)

        val contentIntent = Intent().setClassName(context.packageName, hostActivity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(KRNotificationPayload.KEY_ID, data.id)
            putExtra(KRNotificationPayload.KEY_PAYLOAD, data.payload)
            putExtra(KRNotificationPayload.KEY_ACTION, "default")
        }
        val contentPi = PendingIntent.getActivity(context, data.id, contentIntent, pendingFlags())

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(data.title)
            .setContentText(data.body)
            .setSmallIcon(smallIcon)
            .setContentIntent(contentPi)
            .setAutoCancel(true)

        data.sound?.let { name ->
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId != 0) {
                builder.setSound(Uri.parse("android.resource://${context.packageName}/$resId"))
            }
        }
        data.groupKey?.let { builder.setGroup(it) }

        NotificationManagerCompat.from(context).notify(data.id, builder.build())
        return true
    }

    /** 渠道：不存在则回退 default；default 也不存在则自动创建 */
    private fun resolveChannel(manager: NotificationManager, channelId: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId
        val wanted = channelId.ifEmpty { CHANNEL_DEFAULT }
        if (manager.getNotificationChannel(wanted) != null) return wanted
        if (manager.getNotificationChannel(CHANNEL_DEFAULT) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_DEFAULT, "Default", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        return CHANNEL_DEFAULT
    }

    fun createChannel(manager: NotificationManager, id: String, name: String, importance: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(id, name, mapImportance(importance)).apply {
            description = name
        }
        manager.createNotificationChannel(channel)
    }

    fun mapImportance(importance: String): Int = when (importance) {
        "HIGH" -> NotificationManager.IMPORTANCE_HIGH
        "LOW" -> NotificationManager.IMPORTANCE_LOW
        "MIN" -> NotificationManager.IMPORTANCE_MIN
        else -> NotificationManager.IMPORTANCE_DEFAULT
    }

    fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
}
