package io.github.arialentropy.notification.module

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 定时/重复通知到点后由 AlarmManager 触发，负责构建并展示通知 */
class KRNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        KRNotificationPublisher.publish(context, KRNotificationPayload.fromIntent(intent))
    }
}
