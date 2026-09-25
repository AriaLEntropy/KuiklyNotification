package io.github.arialentropy.notification.module

import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Module 基类，封装 toNative 调用。
 *
 * 子类只关心业务方法名与参数，不需要重复处理 JSON 序列化与同步/异步差异。
 */
abstract class KRNotificationBaseModule : Module() {

    /** 异步调用：Native 通过 callback 回包 `{"code","msg","data"}` */
    protected fun callNative(method: String, data: JSONObject?, callback: CallbackFn?) {
        toNative(false, method, data?.toString(), callback, false)
    }

    /** 常驻调用：callback 不会被自动销毁，需配套 remove 方法释放 */
    protected fun callNativeKeepAlive(method: String, data: JSONObject?, callback: CallbackFn?) {
        toNative(true, method, data?.toString(), callback, false)
    }

    /** 同步调用，返回原始字符串 */
    protected fun syncCallNative(method: String, data: JSONObject?): String? {
        return toNative(false, method, data?.toString(), null, true).returnValue?.toString()
    }

    /** 同步调用并解析为 JSONObject；无结果时返回 null */
    protected fun syncCallNativeJson(method: String, data: JSONObject?): JSONObject? {
        val raw = syncCallNative(method, data) ?: return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "null" || trimmed == "{}") return null
        return JSONObject(trimmed)
    }
}
