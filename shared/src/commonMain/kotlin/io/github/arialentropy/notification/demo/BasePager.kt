package io.github.arialentropy.notification.demo

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.pager.Pager
import io.github.arialentropy.notification.model.NotificationConfig
import io.github.arialentropy.notification.module.KRNotificationModule

/** 注册通知 Module 的基类 Pager，并从 pageData 注入宿主配置 */
internal abstract class BasePager : Pager() {

    override fun createExternalModules(): Map<String, Module>? {
        val modules = hashMapOf<String, Module>()
        modules[KRNotificationModule.MODULE_NAME] = KRNotificationModule()
        return modules
    }

    override fun created() {
        super.created()
        val params = pageData.params
        val config = NotificationConfig(
            hostActivity = params.optString("hostActivity").takeIf { it.isNotEmpty() },
            smallIconResId = params.optInt("smallIconResId").takeIf { it != 0 },
            hostBundleName = params.optString("hostBundleName").takeIf { it.isNotEmpty() },
            hostAbilityName = params.optString("hostAbilityName").takeIf { it.isNotEmpty() }
        )
        acquireModule<KRNotificationModule>(KRNotificationModule.MODULE_NAME).configure(config) { }
    }
}
