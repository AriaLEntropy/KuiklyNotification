package io.github.arialentropy.notification.module

import android.content.Context
import java.lang.ref.WeakReference

/** 当前存活的 Module 弱引用，用于把点击事件分发给常驻监听 */
internal object KRNotificationLaunch {

    private var moduleRef: WeakReference<KRNotificationModule>? = null

    fun attach(module: KRNotificationModule) {
        moduleRef = WeakReference(module)
    }

    fun detach(module: KRNotificationModule) {
        if (moduleRef?.get() === module) {
            moduleRef = null
        }
    }

    fun dispatch(context: Context, id: Int, payload: String?, action: String) {
        moduleRef?.get()?.onNotificationClick(id, payload, action)
    }
}
