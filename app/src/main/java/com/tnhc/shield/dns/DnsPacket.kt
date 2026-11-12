package com.tnhc.shield.dns

import java.nio.ByteBuffer

data class DnsQuery(
    val id: Int,
    val domain: String,
    val qtype: Int,
    val qclass: Int,
    val rawQuery: ByteArray
) {
    val isARecord get() = qtype == 1
    val isAAAARecord get() = qtype == 28
    val isMxRecord get() = qtype == 15
    val isCnameRecord get() = qtype == 5
}

object DnsPacketParser {

    fun parseQuery(data: ByteArray): DnsQuery? = try {
        val buf = ByteBuffer.wrap(data)
        val id = buf.short.toInt() and 0xFFFF
        val flags = buf.short.toInt() and 0xFFFF

        // Must be a query (QR=0)
        if (flags and 0x8000 != 0) return null

        val qdCount = buf.short.toInt() and 0xFFFF
        if (qdCount == 0) return null

        // Skip AN, NS, AR counts
        buf.short; buf.short; buf.short

        // Parse QNAME
        val labels = mutableListOf<String>()
        while (buf.hasRemaining()) {
            val len = buf.get().toInt() and 0xFF
            if (len == 0) break
            if (len > 63 || buf.remaining() < len) return null
            val label = ByteArray(len)
            buf.get(label)
            labels.add(String(label, Charsets.US_ASCII))
        }

        if (buf.remaining() < 4) return null
        val qtype = buf.short.toInt() and 0xFFFF
        val qclass = buf.short.toInt() and 0xFFFF

        DnsQuery(
            id = id,
            domain = labels.joinToString(".").lowercase(),
            qtype = qtype,
            qclass = qclass,
            rawQuery = data
        )
    } catch (e: Exception) {
        null
    }

    fun buildNxdomainResponse(query: ByteArray): ByteArray {
        val response = query.copyOf()
        // QR=1 (response), keep opcode, set RA=1, RCODE=3 (NXDOMAIN)
        response[2] = (response[2].toInt() or 0x80).toByte()
        response[3] = 0x83.toByte() // RA=1, RCODE=3
        // Zero out answer/authority/additional counts
        response[6] = 0; response[7] = 0
        response[8] = 0; response[9] = 0
        response[10] = 0; response[11] = 0
        return response
    }

    fun buildBlockedAResponse(query: ByteArray): ByteArray {
        // Returns 0.0.0.0 for A queries — apps fail fast with no route
        val qBuf = ByteBuffer.wrap(query)
        val id = qBuf.short.toInt() and 0xFFFF

        // Find end of question section
        qBuf.position(12)
        while (qBuf.hasRemaining() && qBuf.get().toInt() and 0xFF != 0) {
            // labels consumed by parse; skip to 0 terminator
        }
        if (qBuf.remaining() < 4) return buildNxdomainResponse(query)
        val qtype = qBuf.short.toInt() and 0xFFFF
        val qclass = qBuf.short.toInt() and 0xFFFF

        if (qtype != 1) return buildNxdomainResponse(query)

        // Rebuild with 1 A-record answer (0.0.0.0, TTL=10)
        val questionSection = query.copyOfRange(12, qBuf.position())
        val buf = ByteBuffer.allocate(12 + questionSection.size + 16)
        buf.putShort(id.toShort())
        buf.putShort(0x8180.toShort()) // QR=1, RD=1, RA=1, NOERROR
        buf.putShort(1) // QDCOUNT=1
        buf.putShort(1) // ANCOUNT=1
        buf.putShort(0) // NSCOUNT
        buf.putShort(0) // ARCOUNT
        buf.put(questionSection)
        // Answer: pointer to question name (0xC00C), type A, class IN, TTL=10, RDLENGTH=4, 0.0.0.0
        buf.putShort(0xC00C.toShort())
        buf.putShort(1) // TYPE A
        buf.putShort(1) // CLASS IN
        buf.putInt(10) // TTL
        buf.putShort(4) // RDLENGTH
        buf.putInt(0) // 0.0.0.0
        return buf.array()
    }
}

data class IpUdpPacket(
    val version: Int,
    val srcIp: ByteArray,
    val dstIp: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val payload: ByteArray,
    val rawPacket: ByteArray
)

object IpPacketParser {

    fun parseUdpPacket(raw: ByteArray): IpUdpPacket? = try {
        val buf = ByteBuffer.wrap(raw)
        val versionIhl = buf.get().toInt() and 0xFF
        val version = versionIhl shr 4
        val ihl = (versionIhl and 0x0F) * 4

        if (version == 4) {
            if (raw.size < ihl + 8) return null
            buf.position(9)
            val protocol = buf.get().toInt() and 0xFF
            if (protocol != 17) return null // UDP only

            buf.position(12)
            val srcIp = ByteArray(4); buf.get(srcIp)
            val dstIp = ByteArray(4); buf.get(dstIp)
            buf.position(ihl)
            val srcPort = buf.short.toInt() and 0xFFFF
            val dstPort = buf.short.toInt() and 0xFFFF
            val udpLen = (buf.short.toInt() and 0xFFFF) - 8
            buf.short // skip checksum
            if (udpLen <= 0 || buf.remaining() < udpLen) return null
            val payload = ByteArray(udpLen); buf.get(payload)

            IpUdpPacket(4, srcIp, dstIp, srcPort, dstPort, payload, raw)
        } else if (version == 6) {
            if (raw.size < 40 + 8) return null
            buf.position(6)
            val nextHeader = buf.get().toInt() and 0xFF
            if (nextHeader != 17) return null

            buf.position(8)
            val srcIp = ByteArray(16); buf.get(srcIp)
            val dstIp = ByteArray(16); buf.get(dstIp)
            val srcPort = buf.short.toInt() and 0xFFFF
            val dstPort = buf.short.toInt() and 0xFFFF
            val udpLen = (buf.short.toInt() and 0xFFFF) - 8
            buf.short
            if (udpLen <= 0 || buf.remaining() < udpLen) return null
            val payload = ByteArray(udpLen); buf.get(payload)

            IpUdpPacket(6, srcIp, dstIp, srcPort, dstPort, payload, raw)
        } else null
    } catch (e: Exception) {
        null
    }

    fun buildUdpResponse(request: IpUdpPacket, dnsResponse: ByteArray): ByteArray {
        return if (request.version == 4) buildIpv4UdpPacket(
            srcIp = request.dstIp,
            dstIp = request.srcIp,
            srcPort = request.dstPort,
            dstPort = request.srcPort,
            payload = dnsResponse
        ) else buildIpv6UdpPacket(
            srcIp = request.dstIp,
            dstIp = request.srcIp,
            srcPort = request.dstPort,
            dstPort = request.srcPort,
            payload = dnsResponse
        )
    }

    private fun buildIpv4UdpPacket(
        srcIp: ByteArray, dstIp: ByteArray,
        srcPort: Int, dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val udpLen = 8 + payload.size
        val ipLen = 20 + udpLen
        val buf = ByteBuffer.allocate(ipLen)

        buf.put(0x45.toByte()) // version=4, IHL=5
        buf.put(0x00.toByte()) // DSCP/ECN
        buf.putShort(ipLen.toShort())
        buf.putShort(0) // ID
        buf.putShort(0x4000.toShort()) // Don't fragment
        buf.put(64.toByte()) // TTL
        buf.put(17.toByte()) // UDP
        buf.putShort(0) // checksum placeholder
        buf.put(srcIp)
        buf.put(dstIp)

        // Compute IP checksum
        val packet = buf.array()
        val checksum = ipv4Checksum(packet, 0, 20)
        packet[10] = (checksum shr 8).toByte()
        packet[11] = checksum.toByte()

        // UDP header
        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putShort(udpLen.toShort())
        buf.putShort(0) // UDP checksum optional for IPv4
        buf.put(payload)

        return buf.array()
    }

    private fun buildIpv6UdpPacket(
        srcIp: ByteArray, dstIp: ByteArray,
        srcPort: Int, dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val udpLen = 8 + payload.size
        val buf = ByteBuffer.allocate(40 + udpLen)

        // IPv6 header
        buf.putInt(0x60000000.toInt()) // version=6, TC=0, flow=0
        buf.putShort(udpLen.toShort()) // payload length
        buf.put(17.toByte()) // next header = UDP
        buf.put(64.toByte()) // hop limit
        buf.put(srcIp)
        buf.put(dstIp)

        // UDP
        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putShort(udpLen.toShort())
        buf.putShort(0)
        buf.put(payload)

        return buf.array()
    }

    private fun ipv4Checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }
}
