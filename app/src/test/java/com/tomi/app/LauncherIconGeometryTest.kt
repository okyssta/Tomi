package com.tomi.app

import android.content.Context
import android.content.res.XmlResourceParser
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class LauncherIconGeometryTest {
    @Test
    fun `foreground stays centered inside the adaptive icon safe circle`() {
        assertSafeGeometry(R.drawable.ic_launcher_foreground)
    }

    @Test
    fun `monochrome foreground stays centered inside the adaptive icon safe circle`() {
        assertSafeGeometry(R.drawable.ic_launcher_monochrome)
    }

    private fun assertSafeGeometry(resourceId: Int) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser: XmlResourceParser = context.resources.getXml(resourceId)
        val bounds = RectF()
        var initialized = false

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.name != "path") continue
            val data = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "pathData")
            val pathBounds = boundsOf(data)
            if (!initialized) {
                bounds.set(pathBounds)
                initialized = true
            } else {
                bounds.union(pathBounds)
            }
        }
        parser.close()

        assertTrue("Foreground has no path geometry", initialized)
        val centerX = bounds.centerX().toDouble()
        val centerY = bounds.centerY().toDouble()
        val corners = listOf(
            bounds.left to bounds.top,
            bounds.right to bounds.top,
            bounds.left to bounds.bottom,
            bounds.right to bounds.bottom,
        )
        val maxCornerRadius = corners.maxOf { (x, y) -> hypot(x.toDouble() - 54.0, y.toDouble() - 54.0) }

        assertTrue("Foreground extends outside the 66dp adaptive safe circle: $maxCornerRadius", maxCornerRadius <= 33.0)
        assertEquals("Foreground bounding box is not horizontally centered", 54.0, centerX, 1.0)
        assertEquals("Foreground bounding box is not vertically centered", 54.0, centerY, 1.0)
    }

    private fun boundsOf(pathData: String): RectF {
        val tokens = Regex("[A-Za-z]|-?\\d+(?:\\.\\d+)?")
            .findAll(pathData)
            .map { it.value }
            .toList()
        val bounds = RectF()
        var index = 0
        var command: Char? = null
        var initialized = false
        fun include(x: Float, y: Float) {
            if (!initialized) {
                bounds.set(x, y, x, y)
                initialized = true
            } else {
                bounds.union(x, y)
            }
        }
        while (index < tokens.size) {
            val token = tokens[index]
            if (token.length == 1 && token[0].isLetter()) {
                command = token[0]
                index++
                if (command == 'Z') break
                continue
            }
            when (command) {
                'M', 'L', 'T' -> {
                    include(tokens[index].toFloat(), tokens[index + 1].toFloat())
                    index += 2
                }
                'A' -> {
                    index += 5
                    include(tokens[index].toFloat(), tokens[index + 1].toFloat())
                    index += 2
                }
                'H' -> {
                    include(tokens[index].toFloat(), bounds.bottom)
                    index++
                }
                'V' -> {
                    include(bounds.right, tokens[index].toFloat())
                    index++
                }
                else -> error("Unsupported path command $command")
            }
        }
        return bounds
    }
}
