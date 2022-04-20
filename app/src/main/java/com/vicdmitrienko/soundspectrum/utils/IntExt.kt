package com.vicdmitrienko.soundspectrum.utils

import android.content.Context
import android.util.TypedValue

fun Int.getSp(context: Context): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics);
}