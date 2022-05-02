package com.vicdmitrienko.soundspectrum.generator

import android.media.AudioFormat
import kotlin.math.PI
import kotlin.math.sin

class WaveGenerator(
    waveFrequencyHz: Double = FREQ_C5,
    waveAmplitude: Float = 1.0f,
    sampleFrequencyHz: Int = 44100,
    sampleSize: Int = 2048,
    sampleFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    val buffer = ByteArray(sampleSize * 2)

    init {
        if (sampleFormat == AudioFormat.ENCODING_PCM_16BIT) {
            val sinCoefficient = PI * 2 / (sampleFrequencyHz / waveFrequencyHz)
            val valueAmplitude = waveAmplitude * Short.MAX_VALUE
            for (i in 0 until sampleSize) {
                val sampleF: Double = sin(i * sinCoefficient) * valueAmplitude
                val sample: Int = sampleF.toInt()
                buffer[i*2  ] = (sample shr 0).toByte()
                buffer[i*2+1] = (sample shr 8).toByte()
            }
        }
    }

    companion object {
        // url: https://en.wikipedia.org/wiki/Piano_key_frequencies
        val FREQ_C8 = 4186.009  // peak = 194
        val FREQ_C7 = 2093.005  // peak =  97
        val FREQ_C6 = 1046.502  // peak =  49
        val FREQ_C5 = 523.2511  // peak =  24
        val FREQ_C4 = 261.6256  // peak =  12
        val FREQ_C3 = 130.8128  // peak =   6
        val FREQ_C2 = 65.40639  // peak =   3
        val FREQ_C1 = 32.70320  // peak =   1
    }
}