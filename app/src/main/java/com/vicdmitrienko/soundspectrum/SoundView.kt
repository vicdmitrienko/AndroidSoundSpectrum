package com.vicdmitrienko.soundspectrum

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioFormat
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.vicdmitrienko.soundspectrum.utils.getSp
import kotlin.math.sin
import kotlin.system.measureTimeMillis

class SoundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {

    // Optimization ways:
    //TODO: Simplify sound wave. Reduce point count or skip some values in buffer

    // Add functionality
    //TODO: Add draw mode "Spectrum"

    //region Settings
    private val tag = this::class.java.simpleName
    private val paintBack = Paint().apply {
        color = Color.rgb(0, 0x07, 0)
    }
    private val paintHashTextSize = 14
    private val paintHash = Paint().apply {
        color = Color.rgb(0, 0x7F, 0)
        strokeWidth = 0f    // hairline
        textSize = paintHashTextSize.getSp(context)
        isAntiAlias = true
    }
    private val paintWave = Paint().apply {
        color = Color.rgb(0, 0xFF, 0)
        strokeWidth = 0f    // hairline
        textSize = paintHashTextSize.getSp(context)
        isAntiAlias = true
    }

    private val sampleRate = 44100
    private val sampleFormat = AudioFormat.ENCODING_PCM_16BIT

    private val paddingHorizontalTxtLabels = 8f
    private val paddingVerticalTxtLabels = 4f
    //endregion

    private var soundWave: ByteArray? = null

    private val drawTimeAvgCount = 100
    private val drawTimes = Array<Int>(drawTimeAvgCount) { 0 }
    private var drawTimeIndex = 0

    // Test function
    private fun getMockWave(): ByteArray {
        val bytes = ByteArray(3000)

        // Make some mock signal
        if (sampleFormat == AudioFormat.ENCODING_PCM_16BIT) {
            val sampleCount = bytes.size / 2
            for (i in 0 until sampleCount) {
                val sampleF: Double = sin(Math.PI * i / sampleCount) * Short.MAX_VALUE
                val sample: Int = sampleF.toInt()
                bytes[i*2  ] = (sample shr 0).toByte()
                bytes[i*2+1] = (sample shr 8).toByte()
            }
        }

        return bytes
    }

    fun setSoundWave(newWave: ByteArray) {
        // Check for overload
        if (isDirty) {
            Log.d(tag, "SoundView is dirty! Skip invalidation.")
            return
        }

        Log.d(tag, "SoundView invalidation...")
        soundWave = newWave
        invalidate()
    }

    //region Calculators
    private fun calcSampleTimeMs(): Float {
        if (soundWave == null) return 0f

        return when (sampleFormat) {
            AudioFormat.ENCODING_PCM_16BIT -> {
                val samples = soundWave!!.size / 2
                1000f * samples / sampleRate
            }
            else ->
                0f
        }
    }

    private fun getMinValue(): Long {
        return when (sampleFormat) {
            AudioFormat.ENCODING_PCM_16BIT -> -32768
            else -> 0
        }
    }
    private fun getMaxValue(): Long {
        return when (sampleFormat) {
            AudioFormat.ENCODING_PCM_16BIT -> 32767
            else -> 0
        }
    }
    //endregion

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val millis = measureTimeMillis {
            drawMesh(canvas)
            if (soundWave != null)
                drawSoundWave(canvas)
        }
        drawTimes[drawTimeIndex++] = millis.toInt()
        if (drawTimeIndex >= drawTimeAvgCount) drawTimeIndex = 0

        drawStat(canvas)

        Log.i(tag, "Draw time $millis ms")
    }

    private fun drawSoundWave(canvas: Canvas) {
        val maxHeight = height.toFloat() - 1f
        val maxWidth  = width.toFloat() - 1f
        val sampleCount = soundWave!!.size / 2
        val halfHeight = maxHeight / 2

        fun sampleNumToX(sampleNum: Int): Float =
            maxWidth * sampleNum / sampleCount
        fun sampleVolToY(sampleVol: Int): Float =
            halfHeight - halfHeight * sampleVol / Short.MAX_VALUE
        fun sample(i: Int): Int {
            val value = (soundWave!![i * 2 + 1].toInt() and 0xFF shl 8) or
                    (soundWave!![i * 2].toInt() and 0xFF)
            return value.toShort().toInt()
        }

        for (i in 1 until sampleCount) {
            canvas.drawLine(
                sampleNumToX(i-1),
                sampleVolToY(sample(i-1)),
                sampleNumToX(i),
                sampleVolToY(sample(i)),
                paintWave
            )
        }
    }

    private fun drawMesh(canvas: Canvas) {
        val maxHeight = height.toFloat() - 1f
        val maxWidth  = width.toFloat() - 1f

        // Fill background
        canvas.drawPaint(paintBack)
        // Mesh
        for (i in 0..10) {
            val y = i * maxHeight / 10
            val x = i * maxWidth / 10

            canvas.drawLine(0f, y, width.toFloat(), y, paintHash)
            canvas.drawLine(x, 0f, x, height.toFloat(), paintHash)
        }
        // Draw text labels
        // - left
        canvas.drawText(
            "0ms",
            paddingHorizontalTxtLabels,
            height / 2f - paddingVerticalTxtLabels,
            paintHash)
        // - right
        val labelRight = String.format("%.0fms", calcSampleTimeMs())
        val textWidth = paintHash.measureText(labelRight)
        canvas.drawText(
            labelRight,
            width - textWidth - paddingHorizontalTxtLabels,
            height / 2f - paddingVerticalTxtLabels,
            paintHash)
        // - top
        val maxValueStr = getMaxValue().toString()
        //val maxValueWidth = paintHash.measureText(maxValueStr)
        val maxValueMetrics = paintHash.fontMetrics
        val maxValueHeight = -maxValueMetrics.ascent
        canvas.drawText(
            maxValueStr,
            width / 2 + paddingHorizontalTxtLabels,
            maxValueHeight + paddingVerticalTxtLabels,
            paintHash)
        // - bottom
        val minValueStr = getMinValue().toString()
        val minValueWidth = paintHash.measureText(minValueStr)
        //val maxValueMetrics = paintHash.fontMetrics
        //val minValueHeight = -maxValueMetrics.ascent
        canvas.drawText(
            minValueStr,
            width / 2 - paddingHorizontalTxtLabels - minValueWidth,
            height - paddingVerticalTxtLabels,
            paintHash)
    }

    private fun drawStat(canvas: Canvas) {
        val textMetrics = paintHash.fontMetrics
        val textHeight = -textMetrics.ascent * 1.5f

        // view width
        canvas.drawText(
            "View width, dp: $width",
            paddingHorizontalTxtLabels,
            paddingVerticalTxtLabels + textHeight,
            paintWave
        )
        // buffer size
        canvas.drawText(
            "Buffer size, bytes: ${soundWave?.size ?: 0}",
            paddingHorizontalTxtLabels,
            paddingVerticalTxtLabels + textHeight * 2,
            paintWave
        )
        // draw time
        val drawTimeAvg = drawTimes.average()
        canvas.drawText(
            "Avg draw time, ms: $drawTimeAvg",
            paddingHorizontalTxtLabels,
            paddingVerticalTxtLabels + textHeight * 3,
            paintWave
        )
    }

}