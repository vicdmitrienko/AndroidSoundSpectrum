package com.vicdmitrienko.soundspectrum

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

// MVP
// 1) Request permissions for microphone
// 2) Get sound wave from microphone
// 3) Show sound wave on screen
// 4) Create View to show sound spectre on screen
//TODO: Learn to fetch notes from sound stream
//TODO: Write test functions with sound wave generation and notes detection

// Links:
// https://github.com/dotH55/Audio_Analyser
// https://developer.android.com/guide/topics/media/mediarecorder
// https://github.com/dasaki/android_fft_minim
// https://www.androidcookbook.info/android-media/visualizing-frequencies.html
// https://stackoverflow.com/questions/5774104/android-audio-fft-to-retrieve-specific-frequency-magnitude-using-audiorecord
// https://github.com/dczyzowski/FourierTransform2/blob/master/app/src/main/java/com/example/damian/fouriertransform/FFT.java

class MainActivity : AppCompatActivity(), MicRecorder.OnSoundBufferUpdate {

    private val tag = MainActivity::class.java.simpleName
    private val recordAudioRequestCode = 123
    private var micRecorder: MicRecorder? = null
    private var soundView: SoundView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissionToRecordAudio()
        }

        soundView = findViewById(R.id.soundView)

    }

    override fun onResume() {
        super.onResume()

        // Test waveFrequencyHz = 44100.0 / 2048.0
        //soundBufferUpdate( WaveGenerator(waveAmplitude = 0.5f).buffer )

        // Start mic listener
        micRecorder = MicRecorder()
        micRecorder?.setOnSoundBufferCallback(this)
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

    override fun soundBufferUpdate(buffer: ByteArray) {
        Log.i(tag, "soundBufferUpdate triggered")
        soundView?.setSoundWave(buffer)
    }

}