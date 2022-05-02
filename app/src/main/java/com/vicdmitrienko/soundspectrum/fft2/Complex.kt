package com.vicdmitrienko.soundspectrum.fft2

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

class Complex(
    val real: Double,
    val imaginary: Double
) {

    constructor(copyFrom: Complex): this(copyFrom.real, copyFrom.imaginary)

    fun magnitude(): Double = this.abs()

    fun abs2(): Double = hypot(real, imaginary)

    fun abs(): Double {
        if (real.isInfinite() || imaginary.isInfinite()) return Double.POSITIVE_INFINITY

        // |value| == sqrt(a^2 + b^2)
        // sqrt(a^2 + b^2) == a/a * sqrt(a^2 + b^2) = a * sqrt(a^2/a^2 + b^2/a^2)
        // Using the above we can factor out the square of the larger component to dodge overflow.
        val c = abs(real)
        val d = abs(imaginary)
        return when {
            c > d -> {
                val r = d / c
                c * sqrt(1.0 + r * r)
            }
            d == 0.0 -> {
                c // c is either 0.0 or NaN
            }
            else -> {
                val r = c / d
                d * sqrt(1.0 + r * r)
            }
        }
    }

    fun phase(): Double = atan2(imaginary, real)

    fun conjugate(): Complex = Complex(real, -imaginary)

    fun reciprocal(): Complex {
        return if (real == 0.0 && imaginary == 0.0)
            ZERO
        else
            ONE.div(this)
    }

    fun add(b: Complex): Complex = Complex(real + b.real, imaginary + b.imaginary)

    fun minus(b: Complex): Complex = Complex(real - b.real, imaginary - b.imaginary)

    fun multiply(right: Complex): Complex = Complex(
        this.real * right.real - this.imaginary * right.imaginary,
        this.real * right.imaginary + this.imaginary * right.real
    )

    fun div(right: Complex): Complex {
        val a = real
        val b = imaginary
        val c = right.real
        val d = right.imaginary

        if (abs(d) < abs(c)) {
            val doc = d / c
            return Complex(
                (a + b * doc) / (c + d * doc),
                (b - a * doc) / (c + d * doc)
            )
        } else {
            val cod = c / d
            return Complex(
                (b + a * cod) / (d + c * cod),
                (-a + b * cod) / (d + c * cod)
            )
        }
    }

    fun div(right: Double): Complex = div( Complex(right, 0.0) )

    override fun toString(): String {
        return when {
            imaginary == 0.0 -> "$real"
            real == 0.0      -> "${imaginary}i"
            imaginary < 0    -> "$real - ${-imaginary}i"
            else             -> "$real + ${imaginary}i"
        }
    }

    companion object {
        val ZERO = Complex(0.0, 0.0)
        val ONE  = Complex(1.0, 0.0)
    }

}