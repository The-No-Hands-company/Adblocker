package com.tnhc.shield.dns

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class DnsPacketParserTest {

    @Test
    fun testParseStandardDnsQuery() {
        // Construct standard DNS A query for "ads.google.com"
        val buf = ByteBuffer.allocate(64)
        buf.putShort(0x1234.toShort()) // ID
        buf.putShort(0x0100.toShort()) // Standard query (RD=1)
        buf.putShort(1.toShort())      // QDCOUNT = 1
        buf.putShort(0.toShort())      // ANCOUNT = 0
        buf.putShort(0.toShort())      // NSCOUNT = 0
        buf.putShort(0.toShort())      // ARCOUNT = 0

        // "ads"
        buf.put(3.toByte())
        buf.put("ads".toByteArray(Charsets.US_ASCII))
        // "google"
        buf.put(6.toByte())
        buf.put("google".toByteArray(Charsets.US_ASCII))
        // "com"
        buf.put(3.toByte())
        buf.put("com".toByteArray(Charsets.US_ASCII))
        // null terminator
        buf.put(0.toByte())

        // QTYPE = 1 (A), QCLASS = 1 (IN)
        buf.putShort(1.toShort())
        buf.putShort(1.toShort())

        val raw = buf.array().copyOf(buf.position())
        val query = DnsPacketParser.parseQuery(raw)

        assertNotNull(query)
        assertEquals(0x1234, query!!.id)
        assertEquals("ads.google.com", query.domain)
        assertEquals(1, query.qtype)
        assertTrue(query.isARecord)
        assertFalse(query.isAAAARecord)
    }

    @Test
    fun testBuildNxdomainResponse() {
        val query = ByteArray(32) { 0 }
        query[0] = 0xAB.toByte()
        query[1] = 0xCD.toByte()

        val response = DnsPacketParser.buildNxdomainResponse(query)
        assertEquals(0xAB.toByte(), response[0])
        assertEquals(0xCD.toByte(), response[1])
        // Flags: QR bit should be 1
        assertTrue((response[2].toInt() and 0x80) != 0)
        // RCODE should be 3 (NXDOMAIN)
        assertEquals(3, response[3].toInt() and 0x0F)
    }
}
