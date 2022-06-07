package com.niew.bodychecker.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Size
import com.niew.bodychecker.MainActivity.Companion.seekBarProgress


object BackgroundHelper {
    fun createBackgroundLineCanvas(bitmap: Bitmap, size: Size, ratio: Float = 1.0f): Canvas {
        val canvas = Canvas(bitmap)

        val intervalDistance = (minIntervalDistance + (MAX_SEEKBAR_VALUE - seekBarProgress)) * ratio
        val paint = paint.apply { strokeWidth = defaultStrokeWidth * ratio }

        var drawPosition = 0f
        while (drawPosition <= size.width) {
            canvas.drawLine(drawPosition, 0f, drawPosition, size.height.toFloat(), paint)
            drawPosition += intervalDistance
        }

        drawPosition = 0f
        while (drawPosition <= size.height) {
            canvas.drawLine(0f, drawPosition, size.width.toFloat(), drawPosition, paint)
            drawPosition += intervalDistance
        }

        return canvas
    }


    const val MAX_SEEKBAR_VALUE = 300
    private var minIntervalDistance = 20f
    private val defaultStrokeWidth = 5f
    private val paint = Paint().apply {
        color = Color.argb(0.5f, 0.8f, 0.8f, 0.8f)
    }
}

