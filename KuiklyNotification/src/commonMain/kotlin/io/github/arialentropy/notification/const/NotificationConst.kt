package io.github.arialentropy.notification.const

/** 通知相关常量与错误码 */
object NotificationConst {

    /** Module 名称，三端注册名必须一致 */
    const val MODULE_NAME = "KRNotificationModule"

    /** 通知重要性（Android importance / 鸿蒙 SlotType 映射） */
    object Importance {
        const val HIGH = "HIGH"
        const val DEFAULT = "DEFAULT"
        const val LOW = "LOW"
        const val MIN = "MIN"
    }

    /** 重复通知间隔 */
    object Interval {
        const val MINUTE = "MINUTE"
        const val HOUR = "HOUR"
        const val HALF_DAY = "HALF_DAY"
        const val DAY = "DAY"
        const val WEEK = "WEEK"
    }

    /** 权限状态 */
    object PermissionStatus {
        const val GRANTED = "GRANTED"
        const val DENIED = "DENIED"
        const val NOT_DETERMINED = "NOT_DETERMINED"
        const val ERROR = "ERROR"
    }

    /** 错误码（JsonResult.code） */
    object ErrorCode {
        const val INVALID_CONFIG = 1001
        const val PERMISSION_DENIED = 1002
        const val PERMISSION_NOT_DETERMINED = 1003
        const val CHANNEL_NOT_FOUND = 1004
        const val CHANNEL_DISABLED = 1005
        const val PAST_TIME_NOT_ALLOWED = 1006
        const val UNSUPPORTED_INTERVAL = 1007
        const val BAD_SOUND_FORMAT = 1008
        const val REMINDER_NOT_ALLOWED = 1009
        const val UNSUPPORTED = 1010
        const val INVALID_REQUEST = 1011
    }
}
