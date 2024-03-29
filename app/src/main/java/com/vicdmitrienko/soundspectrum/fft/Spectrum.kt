package com.vicdmitrienko.soundspectrum.fft

import kotlin.math.*

@Suppress("unused", "RedundantExplicitType", "RemoveExplicitTypeArguments",
    "MemberVisibilityCanBePrivate"
)
class Spectrum(length: Int) {

    private var signal: Signal? = null
    private var spectrum: Array<Complex> = Array(length) { Complex.ZERO }

    constructor(signal: Signal): this(signal.getLength()) {
        recalculateForNewSignal(signal)
    }

    fun recalculate() {
        if (signal != null) {
            for (i in spectrum.indices)
                spectrum[i] = Complex( signal!!.get(i), 0.0 )
            FourierTransform.FFT(spectrum)
        }
    }

    fun recalculateForNewSignal(signal: Signal) {
        this.signal = signal
        recalculate()
    }

    fun getLength() = spectrum.size

    fun get(index: Int): Complex = spectrum[index]

    fun getAbs(index: Int): Double = spectrum[index].abs()

    fun getPhase(index: Int): Double = spectrum[index].phase()

    fun getRealAmplitude(index: Int): Double {
        val amplitude = spectrum[index].abs()

        return if (index == 0)
            amplitude / spectrum.size
        else
            (amplitude * 2) / spectrum.size
    }

    //TODO: Write peak detection more precisely using neighbour values

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

}