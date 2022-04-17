package com.vicdmitrienko.soundspectrum

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

// MVP
//TODO: Научиться получать звук с микрофона устройства
//TODO: Сделать View для отображения спектра на экране
//TODO: Научиться выделать и определять из спектра ноты

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}