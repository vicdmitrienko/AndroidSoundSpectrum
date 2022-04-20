package com.vicdmitrienko.soundspectrum

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

// MVP
// 1) Запрашивать разрешение на доступ к микрофону
// 2) Научиться получать звук с микрофона устройства
//TODO: Сделать View для отображения спектра на экране
//TODO: Научиться выделать и определять из спектра ноты

// Ссылки:
// https://github.com/dotH55/Audio_Analyser
// https://developer.android.com/guide/topics/media/mediarecorder
// https://github.com/dasaki/android_fft_minim
// https://www.androidcookbook.info/android-media/visualizing-frequencies.html
// https://stackoverflow.com/questions/5774104/android-audio-fft-to-retrieve-specific-frequency-magnitude-using-audiorecord
// https://github.com/dczyzowski/FourierTransform2/blob/master/app/src/main/java/com/example/damian/fouriertransform/FFT.java

class MainActivity : AppCompatActivity() {

    private val tag = MainActivity::class.java.simpleName
    private val recordAudioRequestCode = 123
    private var micRecorder: MicRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissionToRecordAudio()
        }

    }

    override fun onResume() {
        super.onResume()

        // Start mic listener
        micRecorder = MicRecorder()
        micRecorder?.startRecord()
    }

    override fun onPause() {
        // Stop mic listener
        micRecorder?.stopRecord()
        micRecorder = null

        super.onPause()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getPermissionToRecordAudio() {
        // 1) Use the support library version ContextCompat.checkSelfPermission(..) to avoid
        // checking the build version since Context.checkSelfPermission(..) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            // The permission is NOT already granted.
            // Check if the user has been asked about this permission already and denied it.
            // If so, we want to give more explanation about why the permission is needed.
            // Fire off an async request to actually get the permission.
            // This will show the standard permission request dialog UI.
            requestPermissions(arrayOf(RECORD_AUDIO), recordAudioRequestCode)

        }
    }

    // Callback with the request from requestPermissions(..)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Make sure it's our original request
        if (requestCode == recordAudioRequestCode) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Record permission granted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "You must give permissions to use this app. App is exiting.", Toast.LENGTH_LONG).show()
                finishAffinity()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}