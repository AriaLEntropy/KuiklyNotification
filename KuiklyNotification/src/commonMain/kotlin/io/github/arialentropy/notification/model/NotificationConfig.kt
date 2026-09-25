package io.github.arialentropy.notification.model

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 宿主初始化配置。
 *
 * - Android 需要 [hostActivity] + [smallIconResId]
 * - 鸿蒙需要 [hostBundleName] + [hostAbilityName]
 * - iOS 无需配置（delegate 机制）
 */
data class NotificationConfig(
    /** Android 入口 Activity 类名 */
    val hostActivity: String? = null,
    /** Android 通知小图标资源 id（R.drawable.xxx） */
    val smallIconResId: Int? = null,
    /** 鸿蒙 bundleName */
    val hostBundleName: String? = null,
    /** 鸿蒙入口 AbilityName */
    val hostAbilityName: String? = null
) {

    fun toJson(): JSONObject = JSONObject().apply {
        hostActivity?.let { put("hostActivity", it) }
        smallIconResId?.let { put("smallIconResId", it) }
        hostBundleName?.let { put("hostBundleName", it) }
        hostAbilityName?.let { put("hostAbilityName", it) }
    }
}
