package com.vicdmitrienko.soundspectrum

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioFormat
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.graphics.withSave
import com.vicdmitrienko.soundspectrum.fft.Signal
import com.vicdmitrienko.soundspectrum.fft.Spectrum
import com.vicdmitrienko.soundspectrum.utils.getSp
import kotlin.math.min
import kotlin.system.measureTimeMillis

@Suppress("RemoveExplicitTypeArguments")
class SoundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {

    // Optimization ways:
    //TODO: Simplify sound wave for fast drawing. Reduce point count or skip some values in buffer

    // Add functionality
    //TODO: Add draw mode switcher between "Wave" and "Spectrum" modes

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
    private var soundSpectrum: Spectrum? = null

    private var viewMode = ViewMode.SPECTRUM

    private var isDraw = false

    private val drawTimeAvgCount = 100
    private val drawTimes = Array<Int>(drawTimeAvgCount) { 0 }
    private var drawTimeIndex = 0

    fun setSoundWave(newWave: ByteArray) {
        // Check for overload
        if (isDraw) {
            Log.d(tag, "SoundView is dirty! Skip invalidation.")
            return
        }

        Log.d(tag, "SoundView invalidation...")
        soundWave = newWave
        soundSpectrum = null
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
    private fun sampleCount(): Int {
        return if (soundWave != null)
            soundWave!!.size / 2
        else
            0
    }
    private fun sample(i: Int): Int {
        val value = (soundWave!![i * 2 + 1].toInt() and 0xFF shl 8) or
                (soundWave!![i * 2].toInt() and 0xFF)
        return value.toShort().toInt()
    }
    //endregion

    override fun onDraw(canvas: Canvas) {
        isDraw = true
        super.onDraw(canvas)

        val millis = measureTimeMillis {
            when (viewMode) {
                ViewMode.WAVE -> {
                    drawWaveMesh(canvas)
                    drawSoundWave(canvas)
                }
                ViewMode.SPECTRUM -> {
                    drawSpectrumMesh(canvas)
                    drawSoundSpectrum(canvas)
                }
            }
        }
        drawTimes[drawTimeIndex++] = millis.toInt()
        if (drawTimeIndex >= drawTimeAvgCount) drawTimeIndex = 0

        drawStat(canvas)

        Log.i(tag, "Draw time $millis ms")
        isDraw = false
    }

    private fun drawSoundSpectrum(canvas: Canvas) {
        if (soundWave == null) return

        if (soundSpectrum == null) {
            /*
             * FFT waits sampling buffer with size of power of 2.
             * We need to append buffer with zeroes.
             */
            val sampleCount = sampleCount()
            var signalLength = 2
            while (signalLength < sampleCount) signalLength *= 2

            val samplesBuf = Array<Double>(signalLength) { i ->
                if (i<sampleCount)
                    sample(i).toDouble()
                else
                    0.0
            }
            val signal = Signal(samplesBuf)
            // create spectrum
            soundSpectrum = Spectrum(signal)

            val strongPeak = soundSpectrum!!.detectStrongPeak(0.0)
            Log.i(tag, "Strong peak = $strongPeak")
        }

        val maxHeight = height.toFloat() - 1f
        val halfHeight = maxHeight / 2
        val maxWidth  = width.toFloat() - 1f
        val sampleMax = 1000

        /* We do not need all calculated spectrum values.
         * Maximum is - signals count of highest note in sampled frame.
         * For example, C8 = 4186.009 Hz
         * in sampling frame 46.4ms (44100Hz sampling, 2048 samples),
         * peak should be around 194 repeats in frame.
         */
        val spectrumSize = soundSpectrum!!.getLength() / 2
        val sampleCount = sampleCount()
        val maxWaveFreqHz = 3000
        val maxWaveCountInFrame: Int = maxWaveFreqHz * sampleCount / sampleRate

        val maxWaveCount = min(spectrumSize, maxWaveCountInFrame)

        fun sampleNumToX(sampleNum: Int): Float =
            maxWidth * sampleNum / maxWaveCount
        fun sampleVolToY(sampleVol: Float): Float =
            halfHeight - halfHeight * sampleVol / sampleMax
        fun spectrumValue(index: Int): Float =
            soundSpectrum!!.getAbs(index).toFloat()

        // Draw frequency values on mesh
        canvas.withSave {
            canvas.rotate(-90f)

            val fontMetrics = paintHash.fontMetrics
            val textHeight = -fontMetrics.ascent

            // Draw first label Hz
            val hzStr = "Hz"
            val hzTxtWidth = paintHash.measureText(hzStr)
            canvas.drawText(
                hzStr,
                -maxHeight / 2 - hzTxtWidth - paddingHorizontalTxtLabels,
                x + textHeight + paddingVerticalTxtLabels,
                paintHash
            )

            for (i in 1..10) {
                val x = i * maxWidth / 10
                val freq: Int = i * maxWaveCount/10 * sampleRate / sampleCount

                val freqStr = freq.toString()
                val freqTxtWidth = paintHash.measureText(freqStr)

                canvas.drawText(
                    freqStr,
                    -maxHeight / 2 - freqTxtWidth - paddingHorizontalTxtLabels,
                    x - paddingVerticalTxtLabels,
                    paintHash
                )
            }
        }

        // Draw spectrum curve
        for (i in 1 until maxWaveCount) {
            canvas.drawLine(
                sampleNumToX(i-1),
                sampleVolToY(spectrumValue(i-1)),
                sampleNumToX(i),
                sampleVolToY(spectrumValue(i)),
                paintWave
            )
        }
    }

    private fun drawSpectrumMesh(canvas: Canvas) {
        val maxHeight = height.toFloat() - 1f
        val maxWidth = width.toFloat() - 1f

        // Fill background
        canvas.drawPaint(paintBack)
        // Mesh
        for (i in 0..10) {
            val y = i * maxHeight / 10
            val x = i * maxWidth / 10

            canvas.drawLine(0f, y, width.toFloat(), y, paintHash)
            canvas.drawLine(x, 0f, x, height.toFloat(), paintHash)
        }
    }

    private fun drawSoundWave(canvas: Canvas) {
        if (soundWave == null) return

        val maxHeight = height.toFloat() - 1f
        val maxWidth  = width.toFloat() - 1f
        val sampleCount = sampleCount()
        val halfHeight = maxHeight / 2

        fun sampleNumToX(sampleNum: Int): Float =
            maxWidth * sampleNum / sampleCount
        fun sampleVolToY(sampleVol: Int): Float =
            halfHeight - halfHeight * sampleVol / Short.MAX_VALUE

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

    private fun drawWaveMesh(canvas: Canvas) {
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

        // view mode
        canvas.drawText(
            "View mode: ${viewMode.desc}",
            paddingHorizontalTxtLabels,
            paddingVerticalTxtLabels + textHeight,
            paintWave
        )
        // view width
        canvas.drawText(
            "View width, dp: $width",
            paddingHorizontalTxtLabels,
            paddingVerticalTxtLabels + textHeight * 2,
            paintWave
        )
        // buffer size
        canvas.drawText(
            "Buffer size, bytes: ${soundWave?.size ?: 0}",
            paddingHorizontalTxtLabels,
            paddingVerticalTxtLabels + textHeight * 3,
            paintWave
        )
        // draw time
        val drawTimeAvg = drawTimes.average()
        canvas.drawText(
            "Avg draw time, ms: $drawTimeAvg",
            paddingHorizontalTxtLabels,
            paddingVerticalTxtLabels + textHeight * 4,
            paintWave
        )
    }

    @Suppress("unused")
    enum class ViewMode(val id: Int, val desc: String) {
        WAVE(0, "Sound wave"),
        SPECTRUM(1, "Sound spectrum")
    }

}