package com.vicdmitrienko.soundspectrum

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log

class MicRecorder : Runnable {

    private val tag = this::class.java.simpleName
    private val sampleRate = 44100
    private var isRecording = false

    @SuppressLint("MissingPermission")
    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

        val bufferSize: Int = AudioRecord.getMinBufferSize(sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(tag, "Buffer Size Error!")
        }
        Log.i(tag, "bufferSize = $bufferSize")

        val audioBuffer = ByteArray(bufferSize)
        val record = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(tag, "Audio Record can't initialize!")
            return
        }
        record.startRecording()
        Log.i(tag, "Start Recording..")
        isRecording = true

        while (isRecording) {
            val numberOfBytes = record.read(audioBuffer, 0, audioBuffer.size)
            Log.i(tag, "Received $numberOfBytes bytes from mic.")
        }

        record.stop()
        record.release()
        Log.i(tag, "Recorder stopped.")

    }

    fun startRecord() {
        val thread = Thread(this)
        thread.start()
    }

    fun stopRecord() {
        isRecording = false
    }

}