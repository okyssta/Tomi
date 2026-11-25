package com.tomi.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalRouteTest {

    @Test
    fun `known routes are accepted after onboarding`() {
        assertEquals("settings", ExternalRoute.resolve("settings", onboardingComplete = true))
        assertEquals("move/WALK", ExternalRoute.resolve("move/WALK", onboardingComplete = true))
    }

    @Test
    fun `unknown and malformed routes are rejected`() {
        assertNull(ExternalRoute.resolve("not-a-destination", onboardingComplete = true))
        assertNull(ExternalRoute.resolve("move/../../settings", onboardingComplete = true))
        assertNull(ExternalRoute.resolve(null, onboardingComplete = true))
    }

    @Test
    fun `external routes cannot bypass onboarding`() {
        assertNull(ExternalRoute.resolve("settings", onboardingComplete = false))
        assertNull(ExternalRoute.resolve("home", onboardingComplete = false))
    }
}
