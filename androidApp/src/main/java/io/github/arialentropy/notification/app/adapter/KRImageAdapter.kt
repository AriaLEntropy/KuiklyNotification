package io.github.arialentropy.notification.app.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import com.tencent.kuikly.core.render.android.KuiklyRenderViewContext
import com.tencent.kuikly.core.render.android.adapter.HRImageLoadOption
import com.tencent.kuikly.core.render.android.adapter.IKRImageAdapter

/**
 * 最小图片适配器：支持 base64 / assets / file，网络图片返回 null（Demo 不需要网络图）。
 * 生产环境可替换为 Glide / Coil 实现。
 */
class KRImageAdapter(private val context: Context) : IKRImageAdapter {

    override fun fetchDrawable(
        imageLoadOption: HRImageLoadOption,
        callback: (drawable: Drawable?) -> Unit,
    ) {
        val src = imageLoadOption.src
        try {
            when {
                src.startsWith("data:image") || src.startsWith("base64") -> {
                    val base64 = src.substringAfter(",", src)
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    callback(BitmapDrawable(context.resources, BitmapFactory.decodeByteArray(bytes, 0, bytes.size)))
                }

                src.startsWith(HRImageLoadOption.SCHEME_ASSETS) -> {
                    val path = src.substring(HRImageLoadOption.SCHEME_ASSETS.length)
                    context.assets.open(path).use { stream ->
                        callback(BitmapDrawable(context.resources, BitmapFactory.decodeStream(stream)))
                    }
                }

                src.startsWith("file://") -> {
                    callback(BitmapDrawable(context.resources, BitmapFactory.decodeFile(src.removePrefix("file://"))))
                }

                else -> callback(null)
            }
        } catch (t: Throwable) {
            callback(null)
        }
    }

    override fun getDrawableWidth(
        kuiklyRenderViewContext: KuiklyRenderViewContext,
        drawable: Drawable,
    ): Float = drawable.intrinsicWidth.toFloat()

    override fun getDrawableHeight(
        kuiklyRenderViewContext: KuiklyRenderViewContext,
        drawable: Drawable,
    ): Float = drawable.intrinsicHeight.toFloat()
}
