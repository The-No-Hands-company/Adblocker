package com.tnhc.shield.dns

import org.junit.Assert.*
import org.junit.Test

class DummyAdResponderTest {

    @Test
    fun testTransparentGifIsValid() {
        val gif = DummyAdResponder.TRANSPARENT_1X1_GIF
        assertEquals(43, gif.size)
        // Check GIF header "GIF89a"
        val header = String(gif.copyOfRange(0, 6), Charsets.US_ASCII)
        assertEquals("GIF89a", header)
        // Trailer must be 0x3B (';')
        assertEquals(0x3B.toByte(), gif.last())
    }

    @Test
    fun testBuildHttpResponseHeaders() {
        val body = DummyAdResponder.EMPTY_JSON
        val response = DummyAdResponder.buildHttpResponse(body, "application/json")
        val responseStr = String(response, Charsets.US_ASCII)

        assertTrue(responseStr.startsWith("HTTP/1.1 200 OK\r\n"))
        assertTrue(responseStr.contains("Content-Type: application/json\r\n"))
        assertTrue(responseStr.contains("Content-Length: ${body.size}\r\n"))
        assertTrue(responseStr.endsWith("{}"))
    }
}
