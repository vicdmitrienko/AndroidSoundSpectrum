package com.vicdmitrienko.soundspectrum.fft

import kotlin.math.sqrt

class Signal(
    private val signalValues: Array<Complex>
) {

    constructor(length: Int): this(
        Array<Complex>(length) { Complex() }
    )

    constructor(copyFrom: Signal): this(copyFrom.signalValues.size) {
        for (i in signalValues.indices) {
            signalValues[i] = Complex(copyFrom.signalValues[i])
        }
    }

    constructor(signalValuesFrom: Array<Double>): this(signalValuesFrom.size) {
        for (i in signalValuesFrom.indices) {
            signalValues[i].set(re = signalValuesFrom[i], im = 0.0)
        }
    }

    constructor(signalValuesFrom: Collection<Double>): this(signalValuesFrom.size) {
        for ((i, signalValue) in signalValuesFrom.withIndex()) {
            signalValues[i].set(re = signalValue, im = 0.0)
        }
    }

    fun getReal(i: Int): Double = signalValues[i].getRe()

    fun getImaginary(i: Int): Double = signalValues[i].getIm()

    fun get(i: Int): Complex = signalValues[i]

    fun setValue(index: Int, value: Complex) {
        signalValues[index].set(value)
    }

    fun getLength(): Int = signalValues.size

    fun realMean(): Double {
        var mean: Double = 0.0

        for (x in signalValues) {
            mean += x.getRe()
        }
        mean /= signalValues.size

        return mean
    }

    fun standardDeviation(): Double {
        val mean: Double = realMean()
        var deviation: Double = 0.0

        for (x in signalValues) {
            deviation += (x.getRe() - mean) * (x.getRe() - mean)
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
            signalValues[i].set(value)
        }

        return this
    }

    fun minus(b: Signal): Signal {
        checkSameSize(b)

        for ((i, value) in b.signalValues.withIndex()) {
            signalValues[i].minus(value)
        }

        return this
    }

    fun multiply(b: Signal): Signal {
        checkSameSize(b)

        for ((i, value) in b.signalValues.withIndex()) {
            signalValues[i].times(value)
        }

        return this
    }

    fun toArray(): Array<Complex> = signalValues

}