package com.vicdmitrienko.soundspectrum.fft

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class FourierTransform {

    @Suppress("unused")
    enum class Direction(val sign: Int) {
        FORWARD(1),
        BACKWARD(-1)
    }

    companion object {


        // One dimensional Fast Fourier Transform.
        @Suppress("RedundantExplicitType", "FunctionName")
        fun FFT(data: Array<Complex>, direction: Direction = Direction.FORWARD) {
            val n = data.size
            val m = log2(n)

            // reorder data first
            reorderData(data)

            // compute FFT
            var tn: Int = 1
            var tm: Int

            for (k in 1..m) {
                val rotation = getComplexRotation(k, direction)

                tm = tn
                tn = tn shl 1

                for (i in 0 until tm) {
                    val t: Complex = rotation[i]

                    for (even in i until n step tn) {
                        val odd = even + tm
                        val ce = data[even]
                        val co = data[odd]

                        val tr: Double = co.real * t.real - co.imaginary * t.imaginary
                        val ti: Double = co.real * t.imaginary + co.imaginary * t.real

                        data[even] = data[even].add( Complex(tr, ti) )
                        data[odd]  = Complex( ce.real - tr, ce.imaginary - ti )
                    }
                }
            }

            if (direction == Direction.FORWARD) {
                for (i in data.indices) {
                    data[i] = data[i].div( n.toDouble() )
                }
            }
        }

        // Get rotation of complex number
        @Suppress("RemoveExplicitTypeArguments")
        private fun getComplexRotation(numberOfBits: Int, direction: Direction): Array<Complex> {
            val directionIndex = if (direction == Direction.FORWARD) 0 else 1

            // check if the array is already calculated
            if (COMPLEX_ROTATION[numberOfBits - 1][directionIndex] == null) {
                val n: Int = 1 shl (numberOfBits - 1)
                var uR = 1.0
                var uI = 0.0
                val angle: Double = PI / n * direction.sign
                val wR = cos(angle)
                val wI = sin(angle)
                var t: Double
                val rotation = Array<Complex>(n) {
                    val rotationI = Complex(uR, uI)
                    t = uR * wI + uI * wR
                    uR = uR * wR - uI * wI
                    uI = t

                    rotationI
                }
                COMPLEX_ROTATION[numberOfBits - 1][directionIndex] = rotation
            }
            return COMPLEX_ROTATION[numberOfBits - 1][directionIndex]!!
        }

        // Reorder data for FFT using
        private fun reorderData(data: Array<Complex>) {
            val len = data.size

            // check data length
            if (len < MIN_LENGTH || len > MAX_LENGTH || !isPowerOf2(len))
                throw RuntimeException("Incorrect data length.")

            val rBits = getReversedBits(log2(len))

            for (i in data.indices) {
                val s = rBits[i]
                if (s > i) {
                    val t = data[i]
                    data[i] = data[s]
                    data[s] = t
                }
            }
        }

        // Get array, indicating which data members should be swapped before FFT
        @Suppress("RemoveExplicitTypeArguments")
        fun getReversedBits(numberOfBits: Int): Array<Int> {
            if (numberOfBits < MIN_BITS || numberOfBits > MAX_BITS)
                throw IllegalArgumentException("getReversedBits: Illegal bits")

            // check if the array is already calculated
            if (RESERVED_BITS[numberOfBits - 1] == null) {
                val n = pow2(numberOfBits)
                val rBits = Array<Int>(n) { i ->
                    var oldBits = i
                    var newBits = 0

                    for (j in 0 until numberOfBits) {
                        newBits = (newBits shl 1) or (oldBits and 1)
                        oldBits = (oldBits shr 1)
                    }

                    newBits
                }
                RESERVED_BITS[numberOfBits - 1] = rBits
            }
            return RESERVED_BITS[numberOfBits - 1]!!
        }

        // Calculates power of 2.
        private fun pow2(power: Int): Int =
            if (power in 0..30) 1 shl power
            else 0

        // Get base of binary logarithm.
        private fun log2(x: Int): Int {
            if (x <= 65536) {
                if (x <= 256) {
                    if (x <= 16) {
                        if (x <= 4) {
                            if (x <= 2) {
                                if (x <= 2) {
                                    return 0
                                }
                                return 1
                            }
                            return 2
                        }
                        if (x <= 8) {
                            return 3
                        }
                        return 4
                    }
                    if (x <= 64) {
                        if (x <= 32) {
                            return 5
                        }
                        return 6
                    }
                    if (x <= 128) {
                        return 7
                    }
                    return 8
                }
                if (x <= 4096) {
                    if (x <= 1024) {
                        if (x <= 512) {
                            return 9
                        }
                        return 10
                    }
                    if (x <= 2048) {
                        return 11
                    }
                    return 12
                }
                if (x <= 16384) {
                    if (x <= 8192) {
                        return 13
                    }
                    return 14
                }
                if (x <= 32768) {
                    return 15
                }
                return 16
            }
            if (x <= 16777216) {
                if (x <= 1048576) {
                    if (x <= 262144) {
                        if (x <= 131072) {
                            return 17
                        }
                        return 18
                    }
                    if (x <= 524288) {
                        return 19
                    }
                    return 20
                }
                if (x <= 4194304) {
                    if (x <= 2097152) {
                        return 21
                    }
                    return 22
                }
                if (x <= 8388608) {
                    return 23
                }
                return 24
            }
            if (x <= 268435456) {
                if (x <= 67108864) {
                    if (x <= 33554432) {
                        return 25
                    }
                    return 26
                }
                if (x <= 134217728) {
                    return 27
                }
                return 28
            }
            if (x <= 1073741824) {
                if (x <= 536870912) {
                    return 29
                }
                return 30
            }
            return 31
        }

        // Checks if the specified integer is power of 2.
        private fun isPowerOf2(x: Int): Boolean =
            if (x > 0) (x and (x - 1)) == 0
            else false

        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 256 * 16 * 8
        private const val MIN_BITS = 1
        private const val MAX_BITS = 15

        private val RESERVED_BITS = Array<Array<Int>?>(MAX_BITS) { null }
        private val COMPLEX_ROTATION = Array<Array<Array<Complex>?>>(MAX_BITS) {
            Array(2) { null }
        }
    }
}