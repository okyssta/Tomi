package com.tomi.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class MoodChipLabelTest {

    @Test
    fun `mood chips have textual accessible labels`() {
        assertEquals("Mood 1, very low", moodChipLabel(1))
        assertEquals("Mood 3, neutral", moodChipLabel(3))
        assertEquals("Mood 5, very good", moodChipLabel(5))
    }
}
