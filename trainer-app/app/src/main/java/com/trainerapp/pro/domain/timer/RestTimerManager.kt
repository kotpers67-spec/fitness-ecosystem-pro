package com.trainerapp.pro.domain.timer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RestTimerManager(private val context: Context) {

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _totalDuration = MutableStateFlow(90)
    val totalDuration: StateFlow<Int> = _totalDuration.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startTimer(seconds: Int) {
        stopTimer()
        _totalDuration.value = seconds
        _remainingSeconds.value = seconds
        _isRunning.value = true

        timerJob = scope.launch {
            while (_remainingSeconds.value > 0 && _isRunning.value) {
                delay(1000L)
                _remainingSeconds.value -= 1
                if (_remainingSeconds.value == 3 || _remainingSeconds.value == 2 || _remainingSeconds.value == 1) {
                    playShortBeep()
                }
            }
            if (_remainingSeconds.value <= 0 && _isRunning.value) {
                _isRunning.value = false
                triggerFinishedAlert()
            }
        }
    }

    fun addTime(seconds: Int) {
        if (_isRunning.value) {
            val newTime = (_remainingSeconds.value + seconds).coerceAtLeast(0)
            _remainingSeconds.value = newTime
            _totalDuration.value = (_totalDuration.value + seconds).coerceAtLeast(newTime)
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isRunning.value = false
        _remainingSeconds.value = 0
    }

    private var notificationToneGen: ToneGenerator? = null
    private var alarmToneGen: ToneGenerator? = null

    private fun getNotificationToneGen(): ToneGenerator? {
        if (notificationToneGen == null) {
            try {
                notificationToneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            } catch (_: Exception) {}
        }
        return notificationToneGen
    }

    private fun getAlarmToneGen(): ToneGenerator? {
        if (alarmToneGen == null) {
            try {
                alarmToneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            } catch (_: Exception) {}
        }
        return alarmToneGen
    }

    fun release() {
        stopTimer()
        try {
            notificationToneGen?.release()
            notificationToneGen = null
            alarmToneGen?.release()
            alarmToneGen = null
        } catch (_: Exception) {}
        scope.cancel()
    }

    private fun playShortBeep() {
        try {
            getNotificationToneGen()?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {}
    }

    private fun triggerFinishedAlert() {
        try {
            getAlarmToneGen()?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 600)
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 400), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(longArrayOf(0, 300, 150, 400), -1)
            }
        } catch (_: Exception) {}
    }
}
