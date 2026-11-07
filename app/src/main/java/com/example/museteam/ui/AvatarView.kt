package com.example.museteam.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.example.museteam.data.AvatarLogic
import java.io.File
import kotlin.math.max

/**
 * Avatar tròn của 1 Muse: có file avatar thật (đã cache) và giải mã được ->
 * vẽ ảnh cắt tròn; không có/hỏng -> vòng gradient + chữ viết tắt theo tên.
 */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var bitmap: Bitmap? = null
    private var letter: String = "?"
    private var gradA: Int = 0xFFFF5DA2.toInt()
    private var gradB: Int = 0xFFA855F7.toInt()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val clipPath = Path()
    private val bounds = RectF()
    private val drawMatrix = Matrix()

    fun setData(name: String, avatarFile: File?) {
        letter = AvatarLogic.initialsFor(name)
        val idx = AvatarLogic.paletteIndexFor(name)
        gradA = AvatarLogic.PALETTE[idx].first
        gradB = AvatarLogic.PALETTE[idx].second
        bitmap = avatarFile?.takeIf { it.exists() && it.length() > 0 }?.let {
            runCatching { BitmapFactory.decodeFile(it.absolutePath) }.getOrNull()
        }
        contentDescription = name
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        bounds.set(0f, 0f, w, h)
        clipPath.reset()
        clipPath.addOval(bounds, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)
        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled) {
            val scale = max(w / bmp.width, h / bmp.height)
            drawMatrix.reset()
            drawMatrix.setScale(scale, scale)
            drawMatrix.postTranslate(
                (w - bmp.width * scale) / 2f,
                (h - bmp.height * scale) / 2f
            )
            canvas.drawBitmap(bmp, drawMatrix, bmpPaint)
        } else {
            fillPaint.shader =
                LinearGradient(0f, 0f, w, h, gradA, gradB, Shader.TileMode.CLAMP)
            canvas.drawRect(bounds, fillPaint)
            fillPaint.shader = null
            textPaint.textSize = h * 0.36f
            val y = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(letter, w / 2f, y, textPaint)
        }
        canvas.restore()
    }
}
