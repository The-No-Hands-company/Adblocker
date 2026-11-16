package com.tnhc.shield.dns

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class IpPacketParserTest {

    @Test
    fun testParseIpv4UdpPacket() {
        val payload = "DNS_PAYLOAD".toByteArray()
        val udpLen = 8 + payload.size
        val ipLen = 20 + udpLen

        val buf = ByteBuffer.allocate(ipLen)
        // IPv4 header
        buf.put(0x45.toByte()) // Version 4, IHL 5
        buf.put(0.toByte())
        buf.putShort(ipLen.toShort())
        buf.putShort(1)
        buf.putShort(0)
        buf.put(64.toByte()) // TTL
        buf.put(17.toByte()) // UDP Protocol
        buf.putShort(0) // Checksum placeholder
        buf.put(byteArrayOf(10, 0, 0, 2)) // Src IP
        buf.put(byteArrayOf(8, 8, 8, 8))   // Dst IP

        // UDP header
        buf.putShort(12345.toShort()) // Src Port
        buf.putShort(53.toShort())    // Dst Port
        buf.putShort(udpLen.toShort())
        buf.putShort(0)

        // Payload
        buf.put(payload)

        val packet = IpPacketParser.parseUdpPacket(buf.array())
        assertNotNull(packet)
        assertEquals(4, packet!!.version)
        assertEquals(12345, packet.srcPort)
        assertEquals(53, packet.dstPort)
        assertArrayEquals(payload, packet.payload)
    }

    @Test
    fun testBuildIpv6UdpResponseChecksum() {
        val srcIp = ByteArray(16) { 1 }
        val dstIp = ByteArray(16) { 2 }
        val request = IpUdpPacket(
            version = 6,
            srcIp = srcIp,
            dstIp = dstIp,
            srcPort = 54321,
            dstPort = 53,
            payload = byteArrayOf(1, 2, 3),
            rawPacket = ByteArray(48)
        )

        val dnsResponse = byteArrayOf(10, 20, 30, 40)
        val response = IpPacketParser.buildUdpResponse(request, dnsResponse)

        assertEquals(40 + 8 + dnsResponse.size, response.size)
        // Checksum in UDP header at offset 46, 47 must not be 0 in IPv6
        val checksum = ((response[46].toInt() and 0xFF) shl 8) or (response[47].toInt() and 0xFF)
        assertNotEquals(0, checksum)
    }

    @Test
    fun testParseIpv4TcpPacketAndBuildRst() {
        val totalLen = 40
        val buf = ByteBuffer.allocate(totalLen)
        buf.put(0x45.toByte())
        buf.put(0.toByte())
        buf.putShort(totalLen.toShort())
        buf.putShort(1)
        buf.putShort(0)
        buf.put(64.toByte())
        buf.put(6.toByte()) // Protocol TCP
        buf.putShort(0)
        buf.put(byteArrayOf(10, 0, 0, 2))
        buf.put(byteArrayOf(34, 192.toByte(), 122, 188.toByte()))

        buf.putShort(50000.toShort()) // Src Port
        buf.putShort(443.toShort())   // Dst Port
        buf.putInt(1000)              // Seq
        buf.putInt(0)                 // Ack
        buf.put((5 shl 4).toByte())   // Data offset: 5
        buf.put(0x02.toByte())        // Flags: SYN (0x02)
        buf.putShort(65535.toShort()) // Window
        buf.putShort(0)
        buf.putShort(0)

        val tcpPacket = IpPacketParser.parseTcpPacket(buf.array())
        assertNotNull(tcpPacket)
        assertEquals(4, tcpPacket!!.version)
        assertEquals(50000, tcpPacket.srcPort)
        assertEquals(443, tcpPacket.dstPort)
        assertEquals(1000L, tcpPacket.seqNumber)
        assertEquals(0x02, tcpPacket.flags)

        val rst = IpPacketParser.buildTcpRstResponse(tcpPacket)
        assertEquals(40, rst.size)
        // Protocol must be TCP (6)
        assertEquals(6, rst[9].toInt() and 0xFF)
        // Src IP must be original Dst IP
        assertArrayEquals(byteArrayOf(34, 192.toByte(), 122, 188.toByte()), rst.copyOfRange(12, 16))
        // Dst IP must be original Src IP
        assertArrayEquals(byteArrayOf(10, 0, 0, 2), rst.copyOfRange(16, 20))
        // TCP RST flags must be RST | ACK (0x14)
        assertEquals(0x14, rst[33].toInt() and 0xFF)
    }
}
