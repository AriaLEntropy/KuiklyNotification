package io.github.arialentropy.notification.ext

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 统一回调结果。
 *
 * Native 回包格式：`{"code": Int, "msg": String, "data": JSONObject?}`
 * - [code] == [SUCCESS] 表示成功，其余为业务错误码
 */
class JsonResult(
    val code: Int,
    val msg: String = "",
    val data: JSONObject? = null
) {

    constructor(result: JSONObject?) : this(
        code = result?.optInt(CODE, FAILED) ?: FAILED,
        msg = result?.optString(MSG) ?: "",
        data = result?.optJSONObject(DATA)
    )

    val isSuccess: Boolean get() = code == SUCCESS

    val isFailed: Boolean get() = code != SUCCESS

    /** 解析 data 为业务模型 */
    fun <T : IResultData> toData(transform: (JSONObject?) -> T?): T? = data?.let(transform)

    override fun toString(): String = "JsonResult(code=$code, msg=$msg)"

    companion object {
        private const val CODE = "code"
        private const val MSG = "msg"
        private const val DATA = "data"

        const val SUCCESS = 0
        const val FAILED = -1
    }
}

/** data 解析接口 */
interface IResultData {
    fun decode(data: JSONObject?): IResultData?
}

/** 统一异步回调 */
typealias JsonResultCallback = (JsonResult) -> Unit
