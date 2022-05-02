package com.vicdmitrienko.soundspectrum.fft

import kotlin.math.*

class Spectrum(length: Int) {

    private var signal: Signal? = null
    private var spectrum: Array<Complex> = Array(length) { Complex() }

    constructor(signal: Signal): this(signal.getLength()) {
        recalcForNewSignal(signal)
    }

    fun recalc() {
        if (signal != null) spectrum = fft(signal!!.toArray())
    }

    fun recalcForNewSignal(signal: Signal) {
        this.signal = signal
        recalc()
    }

    fun getLength() = spectrum.size

    fun get(index: Int): Complex = spectrum[index]

    fun getAbs(index: Int): Double = spectrum[index].abs()

    fun getPhase(index: Int): Double = spectrum[index].arg()

    fun getRealAmplitude(index: Int): Double {
        val amplitude = spectrum[index].abs()

        return if (index == 0)
            amplitude / spectrum.size
        else
            (amplitude * 2) / spectrum.size
    }

    fun detectStrongPeak(min: Double): Int {
        var peak = -1
        var spectrumPeakAbs: Double = 0.0

        for (i in 0 until spectrum.size / 2) {
            if (peak == -1) {
                val spectrumAbs: Double = spectrum[i].abs()
                if ((spectrumAbs * 2 / spectrum.size) > min) {
                    peak = i
                    spectrumPeakAbs = spectrumAbs
                }
            } else {
                val spectrumAbs: Double = spectrum[i].abs()
                if (spectrumAbs > spectrumPeakAbs
                    && (spectrumAbs * 2 / spectrum.size) > min
                ) {
                    peak = i
                    spectrumPeakAbs = spectrumAbs
                }
            }
        }

        return peak
    }

    fun getAverageAmplitudeIn(harmonic: Int, windowSize: Int): Double {
        val min: Int = max(harmonic - windowSize, 0)
        val max: Int = min(harmonic + windowSize, spectrum.size - 1)

        var avgAmplitude = 0.0
        for (i in min..max) {
            if (i != harmonic)
                avgAmplitude += getRealAmplitude(i)
        }

        return avgAmplitude / (max - min - 1)
    }

    fun getDistributionFunction(confidence: Int): Array<Int> {
        val amplitudeStep: Double = getRealAmplitude( detectStrongPeak(0.0) ) / confidence
        val distributionFunction = Array<Int>(confidence) { 0 }

        for (i in spectrum.indices) {
            val distributionPosition = min(
                ceil(getRealAmplitude(i) / amplitudeStep).toInt(),
                confidence
            )

            for (j in 0 until distributionPosition)
                distributionFunction[j]++
        }

        return distributionFunction
    }

    fun getDistributionDensity(confidence: Int): Array<Int> {
        val function: Array<Int> = getDistributionFunction(confidence)
        val density = Array<Int>(confidence - 1) { i ->
            function[i+1] - function[i]
        }
        return density
    }

    fun estimatedNoise(distributionConfidence: Int, peakTolerance: Int, maxDistance: Int): Double {
        val density: Array<Int> = getDistributionDensity(distributionConfidence)
        var peak: Int = 0

        for (i in density.indices) {
            if (density[i] > density[peak]) peak = i
        }

        val startingPeak = peak
        var currentTolerance: Int = 0

        while (currentTolerance < peakTolerance && (peak - startingPeak) < maxDistance) {
            if (density[peak + 1] > density[peak])
                currentTolerance++
            peak++
        }

        return peak * getRealAmplitude( detectStrongPeak(0.0) ) / distributionConfidence
    }

    companion object {

        @Suppress("UnnecessaryVariable", "LocalVariableName")
        fun fft(x: Array<Complex>): Array<Complex> {
            val N = x.size

            // base case
            if (N == 1) return Array(1) { x[0] }

            // radix 2 Cooley-Tukey FFT
            if (N % 2 != 0) throw RuntimeException("N is not a power of 2")

            // fft of even terms
            val even = Array<Complex>(N / 2) { k -> x[2*k] }
            val q = fft(even)

            // fft of odd terms
            val odd = even // reuse the array
            for (k in odd.indices) { odd[k] = x[2*k + 1] }
            val r = fft(odd)

            // combine
            val y = Array<Complex>(N) { Complex() }
            for (k in 0 until N/2) {
                val kth: Double = -2 * k * PI / N
                val wk          = Complex( cos(kth), sin(kth) )
                val times       = Complex.times(wk, r[k])
                y[k      ].set( Complex.plus( q[k], times) )
                y[k + N/2].set( Complex.minus(q[k], times) )
            }

            return y
        }

    }

}