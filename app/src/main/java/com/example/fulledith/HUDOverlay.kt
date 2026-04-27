package com.example.fulledith

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class HUDOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var isListening = false
    private var volumeLevel = 0f
    private var pulseAngle = 0f
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF41")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF41")
        textSize = 38f
        typeface = Typeface.MONOSPACE
    }
    private val dimTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8800FF41")
        textSize = 28f
        typeface = Typeface.MONOSPACE
    }
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF41")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    init { startAnimation() }

    private fun startAnimation() {
        handler.post(object : Runnable {
            override fun run() {
                pulseAngle = (pulseAngle + 3f) % 360f
                invalidate()
                handler.postDelayed(this, 16)
            }
        })
    }

    fun setListening(listening: Boolean) { isListening = listening; invalidate() }
    fun setVolume(rms: Float) { volumeLevel = (rms + 2f).coerceIn(0f, 12f) / 12f; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val margin = 30f
        val cornerLen = 60f
        greenPaint.strokeWidth = 3f
        canvas.drawLine(margin, margin, margin + cornerLen, margin, greenPaint)
        canvas.drawLine(margin, margin, margin, margin + cornerLen, greenPaint)
        canvas.drawLine(w - margin, margin, w - margin - cornerLen, margin, greenPaint)
        canvas.drawLine(w - margin, margin, w - margin, margin + cornerLen, greenPaint)
        canvas.drawLine(margin, h - margin, margin + cornerLen, h - margin, greenPaint)
        canvas.drawLine(margin, h - margin, margin, h - margin - cornerLen, greenPaint)
        canvas.drawLine(w - margin, h - margin, w - margin - cornerLen, h - margin, greenPaint)
        canvas.drawLine(w - margin, h - margin, w - margin, h - margin - cornerLen, greenPaint)
        textPaint.textSize = 48f
        canvas.drawText("E.D.I.T.H.", 55f, 95f, textPaint)
        dimTextPaint.textSize = 24f
        canvas.drawText("EVEN DEAD I'M THE HERO", 55f, 125f, dimTextPaint)
        val cx = w / 2f
        val cy = h / 2f
        val baseRadius = 80f
        if (isListening) {
            for (i in 1..5) {
                val radius = baseRadius + i * 30f + volumeLevel * 40f * sin(Math.toRadians((pulseAngle + i * 20).toDouble())).toFloat()
                circlePaint.alpha = (255 - i * 40).coerceIn(30, 255)
                canvas.drawCircle(cx, cy, radius, circlePaint)
            }
            greenPaint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, 10f, greenPaint)
            greenPaint.style = Paint.Style.STROKE
            textPaint.textSize = 32f
            val tw = textPaint.measureText("● DİNLİYORUM")
            canvas.drawText("● DİNLİYORUM", cx - tw / 2, cy + baseRadius + 80f, textPaint)
        } else {
            val sweepX = cx + (baseRadius + 60f) * cos(Math.toRadians(pulseAngle.toDouble())).toFloat()
            val sweepY = cy + (baseRadius + 60f) * sin(Math.toRadians(pulseAngle.toDouble())).toFloat()
            canvas.drawLine(cx, cy, sweepX, sweepY, greenPaint)
            canvas.drawCircle(cx, cy, baseRadius + 60f, circlePaint)
        }
        dimTextPaint.textSize = 26f
        canvas.drawText("SYS: ONLINE  |  AI: AKTIF  |  CAM: HAZIR", 30f, h - 80f, dimTextPaint)
        canvas.drawText("v2.0  |  STARK TECH  |  CLASSIFIED", 30f, h - 50f, dimTextPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }
}
