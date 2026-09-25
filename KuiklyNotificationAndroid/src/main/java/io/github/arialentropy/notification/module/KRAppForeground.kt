package io.github.arialentropy.notification.module

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle

/**
 * 轻量前后台判断（避免额外依赖 lifecycle-process）。
 *
 * 通过 Application 的 Activity 生命周期回调统计处于 started 状态的 Activity 数量。
 */
internal object KRAppForeground {

    private var startedCount = 0
    private var registered = false

    val isForeground: Boolean get() = startedCount > 0

    fun ensureRegistered(context: Context) {
        if (registered) return
        val application = context.applicationContext as? Application ?: return
        synchronized(this) {
            if (registered) return
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedCount++
                }

                override fun onActivityStopped(activity: Activity) {
                    if (startedCount > 0) startedCount--
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            })
            registered = true
        }
    }
}
