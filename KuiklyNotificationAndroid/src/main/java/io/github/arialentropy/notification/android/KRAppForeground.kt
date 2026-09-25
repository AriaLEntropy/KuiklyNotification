package io.github.arialentropy.notification.android

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * 前后台判断。
 *
 * 使用 [ProcessLifecycleOwner]，它在 App 启动时由 lifecycle-process 自动注册并已同步当前状态，
 * 不存在“懒注册导致首次判断错误”的问题。
 */
internal object KRAppForeground {

    /** 兼容旧调用点；ProcessLifecycleOwner 无需手动注册 */
    fun ensureRegistered(context: Context) {
        // no-op
    }

    val isForeground: Boolean
        get() = try {
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        } catch (t: Throwable) {
            false
        }
}
