package com.vicdmitrienko.soundspectrum

import com.vicdmitrienko.soundspectrum.fft2.FourierTransform
import org.junit.Test

import org.junit.Assert.*

class FourierTransformTest {

    private fun <T> printArray(arr: Array<T>) {
        print("Array(${arr.size}) = {")
        print(arr.joinToString(", "))
        println("}")
    }

    @Test
    fun reservedBits() {
        val rb1 = FourierTransform.getReversedBits(1)
        printArray(rb1)
        assertEquals(2, rb1.size)
        assertArrayEquals( arrayOf(0, 1), rb1 )

        val rb2 = FourierTransform.getReversedBits(2)
        printArray(rb2)
        assertEquals(4, rb2.size)
        assertArrayEquals( arrayOf(0, 2, 1, 3), rb2 )

        val rb3 = FourierTransform.getReversedBits(3)
        printArray(rb3)
        assertEquals(8, rb3.size)
        assertArrayEquals( arrayOf(0, 4, 2, 6, 1, 5, 3, 7), rb3 )

        val rb4 = FourierTransform.getReversedBits(4)
        printArray(rb4)
        assertEquals(16, rb4.size)
        assertArrayEquals( arrayOf(0, 8, 4, 12, 2, 10, 6, 14, 1, 9, 5, 13, 3, 11, 7, 15), rb4 )
    }
}