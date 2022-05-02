package com.vicdmitrienko.soundspectrum.fft

import kotlin.math.atan2
import kotlin.math.hypot

class Complex(
    // the real or imaginary part
    private var re: Double = 0.0,
    private var im: Double = 0.0
) {

    constructor(copyFrom: Complex) : this(
        re = copyFrom.re,
        im = copyFrom.im
    )

    override fun toString(): String {
        return when {
            im == 0.0 -> "$re"
            re == 0.0 -> "${im}i"
            im < 0    -> "$re - ${-im}i"
            else      -> "$re + ${im}i"
        }
    }

    fun abs(): Double = hypot(re, im)

    fun arg(): Double = atan2(im, re)

    fun plus(b: Complex): Complex {
        re += b.re
        im += b.im
        return this
    }

    fun minus(b: Complex): Complex {
        re -= b.re
        im -= b.im
        return this
    }

    fun times(b: Complex): Complex {
        val real = re * b.re - im * b.im
        val imag = re * b.im + im * b.re

        re = real
        im = imag

        return this
    }

    fun times(alpha: Double): Complex {
        re *= alpha
        im *= alpha
        return this
    }

    fun divides(b: Complex): Complex = times(reciprocal(b))

    fun conjugate(): Complex {
        im = -im
        return this
    }

    fun reciprocal(): Complex {
        val scale = re * re + im * im
        re /= scale
        im /= (-scale)
        return this
    }

    fun getRe(): Double = re

    fun getIm(): Double = im

    fun set(re: Double, im: Double): Complex {
        this.re = re
        this.im = im
        return this
    }

    fun set(b: Complex): Complex {
        this.re = b.re
        this.im = b.im
        return this
    }

    companion object {
        fun plus(a: Complex, b: Complex) = Complex(a).plus(b)
        fun minus(a: Complex, b: Complex) = Complex(a).minus(b)
        fun times(a: Complex, b: Complex) = Complex(a).times(b)
        fun times(a: Complex, alpha: Double) = Complex(a).times(alpha)
        fun divides(a: Complex, b: Complex) = Complex(a).divides(b)
        fun conjugate(a: Complex) = Complex(a).conjugate()
        fun reciprocal(a: Complex) = Complex(a).reciprocal()
    }

}