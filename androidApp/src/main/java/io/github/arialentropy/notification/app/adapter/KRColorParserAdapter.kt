package io.github.arialentropy.notification.app.adapter

import android.content.Context
import android.graphics.Color
import com.tencent.kuikly.core.render.android.adapter.IKRColorParserAdapter

class KRColorParserAdapter(private val context: Context) : IKRColorParserAdapter {

    override fun toColor(colorStr: String): Int? {
        return try {
            if (colorStr.startsWith("#")) {
                Color.parseColor(colorStr)
            } else {
                colorStr.toLongOrNull()?.toInt()
            }
        } catch (t: Throwable) {
            null
        }
    }
}
