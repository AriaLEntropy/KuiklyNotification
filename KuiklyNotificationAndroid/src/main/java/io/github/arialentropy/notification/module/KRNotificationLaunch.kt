package io.github.arialentropy.notification.module

import android.content.Context
import android.content.Intent
import java.lang.ref.WeakReference

/** 通知点击入口与当前 Module 弱引用 */
object KRNotificationLaunch {

    private var moduleRef: WeakReference<KRNotificationModule>? = null

    fun attach(module: KRNotificationModule) {
        moduleRef = WeakReference(module)
    }

    fun detach(module: KRNotificationModule) {
        if (moduleRef?.get() === module) {
            moduleRef = null
        }
    }

    /**
     * 宿主入口 Activity 在 onCreate / onNewIntent 调用。
     * 冷启动时缓存 payload；App 存活时直接分发给当前 Module。
     */
    fun onNewIntent(context: Context, intent: Intent?) {
        val i = intent ?: return
        val id = i.getIntExtra(KRNotificationPayload.KEY_ID, -1)
        if (id < 0) {
            return
        }
        val payload = i.getStringExtra(KRNotificationPayload.KEY_PAYLOAD)
        val action = i.getStringExtra(KRNotificationPayload.KEY_ACTION) ?: "default"
        KRNotificationStore(context).saveLaunch(id, payload, action)
        moduleRef?.get()?.onNotificationClick(id, payload, action)
    }
}
