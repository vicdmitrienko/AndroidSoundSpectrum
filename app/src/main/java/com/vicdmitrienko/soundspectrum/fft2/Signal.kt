package com.vicdmitrienko.soundspectrum.fft2

import kotlin.math.sqrt

class Signal(
    private val signalValues: Array<Double>
) {

    constructor(length: Int): this(
        Array<Double>(length) { 0.0 }
    )

    constructor(copyFrom: Signal): this(copyFrom.signalValues.size) {
        for (i in signalValues.indices) {
            signalValues[i] = copyFrom.signalValues[i]
        }
    }

    constructor(signalValuesFrom: Collection<Double>): this(signalValuesFrom.size) {
        for ((i, signalValue) in signalValuesFrom.withIndex()) {
            signalValues[i] = signalValue
        }
    }

    fun get(i: Int): Double = signalValues[i]

    fun setValue(index: Int, value: Double) {
        signalValues[index] = value
    }

    fun getLength(): Int = signalValues.size

    fun realMean(): Double {
        var mean: Double = 0.0

        for (x in signalValues) {
            mean += x
        }
        mean /= signalValues.size

        return mean
    }

    fun standardDeviation(): Double {
        val mean: Double = realMean()
        var deviation: Double = 0.0

        for (x in signalValues) {
            deviation += (x - mean) * (x - mean)
        }
        deviation /= signalValues.size

        return sqrt(deviation)
    }

    private fun checkSameSize(b: Signal) {
        if (getLength() != b.getLength()) throw RuntimeException("Signals are not the same length!")
    }

    fun set(b: Signal): Signal {
        checkSameSize(b)

        for ((i, value) in b.signalValues.withIndex()) {
            signalValues[i] = value
        }

        return this
    }

    fun minus(b: Signal): Signal {
        checkSameSize(b)

        for ((i, value) in b.signalValues.withIndex()) {
            signalValues[i] = signalValues[i].minus(value)
        }

        return this
    }

    fun multiply(b: Signal): Signal {
        checkSameSize(b)

        for ((i, value) in b.signalValues.withIndex()) {
            signalValues[i] = signalValues[i] * value
        }

        return this
    }

    fun toArray(): Array<Double> = signalValues

}