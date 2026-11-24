package com.tomi.app.domain.movement

class MovementTimer {
    private var startedAtElapsedMs: Long? = null
    private var accumulatedMs = 0L
    var running: Boolean = false
        private set

    fun start(nowElapsedMs: Long) {
        if (!running) {
            startedAtElapsedMs = nowElapsedMs
            running = true
        }
    }

    fun resume(nowElapsedMs: Long) = start(nowElapsedMs)

    fun pause(nowElapsedMs: Long) {
        if (!running) return
        accumulatedMs += (nowElapsedMs - requireNotNull(startedAtElapsedMs)).coerceAtLeast(0L)
        startedAtElapsedMs = null
        running = false
    }

    fun elapsedSeconds(nowElapsedMs: Long): Int {
        val elapsed = accumulatedMs + if (running) {
            (nowElapsedMs - requireNotNull(startedAtElapsedMs)).coerceAtLeast(0L)
        } else {
            0L
        }
        return (elapsed / 1_000L).toInt()
    }

    fun finish(nowElapsedMs: Long): Int {
        val seconds = elapsedSeconds(nowElapsedMs)
        running = false
        startedAtElapsedMs = null
        accumulatedMs = 0L
        return seconds
    }
}
