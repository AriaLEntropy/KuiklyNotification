package io.github.arialentropy.notification.demo

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.pager.Pager
import io.github.arialentropy.notification.module.KRNotificationModule

/** 注册通知 Module 的基类 Pager */
internal abstract class BasePager : Pager() {

    override fun createExternalModules(): Map<String, Module>? {
        val modules = hashMapOf<String, Module>()
        modules[KRNotificationModule.MODULE_NAME] = KRNotificationModule()
        return modules
    }
}
