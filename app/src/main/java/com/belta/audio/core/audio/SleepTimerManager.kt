package com.belta.audio.core.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SleepTimerManager(
    private val scope: CoroutineScope,
    private val onTimerFinished: () -> Unit
) {
    private var timerJob: Job? = null
    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    val isActive: Boolean
        get() = timerJob?.isActive == true && _remainingSeconds.value > 0

    fun startTimer(minutes: Int) {
        cancelTimer()
        val totalSeconds = minutes * 60L
        _remainingSeconds.value = totalSeconds

        timerJob = scope.launch(Dispatchers.Default) {
            var current = totalSeconds
            while (current > 0) {
                delay(1000L)
                current--
                _remainingSeconds.value = current
            }
            _remainingSeconds.value = 0
            withContext(Dispatchers.Main) {
                onTimerFinished()
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = 0
    }
}
