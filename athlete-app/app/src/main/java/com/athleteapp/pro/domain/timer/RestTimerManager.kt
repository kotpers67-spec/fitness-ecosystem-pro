package com.athleteapp.pro.domain.timer

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

data class RestTimerState(
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 90,
    val isRunning: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f
}

class RestTimerManager(private val context: Context) {

    private val _timerState = MutableStateFlow(RestTimerState())
    val timerState: StateFlow<RestTimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startTimer(seconds: Int) {
        stopTimer()
        _timerState.value = RestTimerState(remainingSeconds = seconds, totalSeconds = seconds, isRunning = true)

        timerJob = scope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                delay(1000L)
                val nextSec = _timerState.value.remainingSeconds - 1
                _timerState.value = _timerState.value.copy(remainingSeconds = nextSec)
                if (nextSec in 1..3) {
                    playShortBeep()
                }
            }
            if (_timerState.value.remainingSeconds <= 0 && _timerState.value.isRunning) {
                _timerState.value = _timerState.value.copy(isRunning = false)
                triggerFinishedAlert()
            }
        }
    }

    fun addSeconds(seconds: Int) {
        val current = _timerState.value
        val newRem = (current.remainingSeconds + seconds).coerceAtLeast(0)
        val newTot = (current.totalSeconds + seconds).coerceAtLeast(newRem)
        _timerState.value = current.copy(remainingSeconds = newRem, totalSeconds = newTot)
    }

    fun resetTimer() {
        val tot = _timerState.value.totalSeconds
        startTimer(if (tot > 0) tot else 90)
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = RestTimerState(remainingSeconds = 0, totalSeconds = 0, isRunning = false)
    }

    @Volatile
    private var notificationToneGen: ToneGenerator? = null
    @Volatile
    private var alarmToneGen: ToneGenerator? = null

    @Synchronized
    private fun getNotificationToneGen(): ToneGenerator? {
        if (notificationToneGen == null) {
            try {
                notificationToneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            } catch (_: Exception) {}
        }
        return notificationToneGen
    }

    @Synchronized
    private fun getAlarmToneGen(): ToneGenerator? {
        if (alarmToneGen == null) {
            try {
                alarmToneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            } catch (_: Exception) {}
        }
        return alarmToneGen
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

    @Synchronized
    fun release() {
        stopTimer()
        scope.cancel()
        try {
            notificationToneGen?.release()
        } catch (_: Exception) {}
        notificationToneGen = null

        try {
            alarmToneGen?.release()
        } catch (_: Exception) {}
        alarmToneGen = null
    }
}
