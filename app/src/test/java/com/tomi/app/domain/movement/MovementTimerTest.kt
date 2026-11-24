package com.tomi.app.domain.movement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementTimerTest {

    @Test
    fun `timer excludes paused time`() {
        val timer = MovementTimer()
        timer.start(1_000L)
        timer.pause(6_000L)
        assertEquals(5, timer.elapsedSeconds(6_000L))

        timer.resume(10_000L)
        timer.pause(12_000L)
        assertEquals(7, timer.elapsedSeconds(12_000L))
        assertFalse(timer.running)
    }

    @Test
    fun `timer reports live elapsed time while running`() {
        val timer = MovementTimer()
        timer.start(1_000L)

        assertEquals(9, timer.elapsedSeconds(10_000L))
        assertTrue(timer.running)
    }

    @Test
    fun `finishing returns elapsed seconds and stops the timer`() {
        val timer = MovementTimer()
        timer.start(1_000L)

        val seconds = timer.finish(8_500L)

        assertEquals(7, seconds)
        assertFalse(timer.running)
        assertEquals(0, timer.elapsedSeconds(20_000L))
    }
}
