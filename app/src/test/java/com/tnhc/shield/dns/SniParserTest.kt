package com.tnhc.shield.dns

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class SniParserTest {

    @Test
    fun testParseValidClientHelloSni() {
        val hostname = "ads.unity3d.com"
        val hostBytes = hostname.toByteArray(Charsets.US_ASCII)

        // Construct a synthetic TLS 1.2 / 1.3 ClientHello with SNI extension
        val extBuf = ByteBuffer.allocate(128)
        extBuf.putShort(0x0000.toShort()) // Extension: server_name
        extBuf.putShort((hostBytes.size + 5).toShort()) // Extension length
        extBuf.putShort((hostBytes.size + 3).toShort()) // Server Name List length
        extBuf.put(0x00.toByte()) // host_name type
        extBuf.putShort(hostBytes.size.toShort()) // Host length
        extBuf.put(hostBytes)

        val extBytes = extBuf.array().copyOf(extBuf.position())

        // Handshake body
        val hsBuf = ByteBuffer.allocate(256)
        hsBuf.putShort(0x0303.toShort()) // Client Version TLS 1.2
        hsBuf.put(ByteArray(32) { 0xAA.toByte() }) // Random
        hsBuf.put(0.toByte()) // Session ID len = 0
        hsBuf.putShort(2.toShort()) // Cipher suites len = 2
        hsBuf.putShort(0x1301.toShort()) // TLS_AES_128_GCM_SHA256
        hsBuf.put(1.toByte()) // Compression methods len = 1
        hsBuf.put(0.toByte()) // null compression
        hsBuf.putShort(extBytes.size.toShort()) // Extensions length
        hsBuf.put(extBytes)

        val hsBody = hsBuf.array().copyOf(hsBuf.position())

        // Complete TLS Record
        val recordBuf = ByteBuffer.allocate(512)
        recordBuf.put(0x16.toByte()) // ContentType = Handshake
        recordBuf.putShort(0x0301.toShort()) // Legacy Record Version
        recordBuf.putShort((4 + hsBody.size).toShort()) // Record length
        recordBuf.put(0x01.toByte()) // Handshake type = ClientHello
        recordBuf.put((hsBody.size shr 16).toByte())
        recordBuf.put((hsBody.size shr 8).toByte())
        recordBuf.put((hsBody.size and 0xFF).toByte())
        recordBuf.put(hsBody)

        val fullRecord = recordBuf.array().copyOf(recordBuf.position())

        val extracted = SniParser.parseSni(fullRecord)
        assertEquals("ads.unity3d.com", extracted)
    }

    @Test
    fun testParseInvalidNonTlsPacket() {
        val nonTls = "GET / HTTP/1.1\r\nHost: example.com\r\n\r\n".toByteArray()
        val result = SniParser.parseSni(nonTls)
        assertNull(result)
    }
}
