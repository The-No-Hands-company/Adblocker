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
}

object DnsPacketParser {

    fun parseQuery(data: ByteArray): DnsQuery? {
        return try {
            val buf = ByteBuffer.wrap(data)
            val id = buf.short.toInt() and 0xFFFF
            val flags = buf.short.toInt() and 0xFFFF
            if (flags and 0x8000 != 0) return null  // must be a query

            val qdCount = buf.short.toInt() and 0xFFFF
            if (qdCount == 0) return null
            buf.short; buf.short; buf.short  // skip AN, NS, AR counts

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
    }

    fun buildNxdomainResponse(query: ByteArray): ByteArray {
        val response = query.copyOf()
        response[2] = (response[2].toInt() or 0x80).toByte()
        response[3] = 0x83.toByte() // RA=1, RCODE=3
        response[6] = 0; response[7] = 0
        response[8] = 0; response[9] = 0
        response[10] = 0; response[11] = 0
        return response
    }

    fun buildBlockedAResponse(query: ByteArray): ByteArray {
        return try {
            val qBuf = ByteBuffer.wrap(query)
            val id = qBuf.short.toInt() and 0xFFFF
            qBuf.position(12)
            while (qBuf.hasRemaining()) {
                val len = qBuf.get().toInt() and 0xFF
                if (len == 0) break
                if (len and 0xC0 == 0xC0) { qBuf.get(); break }
                if (qBuf.remaining() < len) return buildNxdomainResponse(query)
                qBuf.position(qBuf.position() + len)
            }
            if (qBuf.remaining() < 4) return buildNxdomainResponse(query)
            val qtype = qBuf.short.toInt() and 0xFFFF
            if (qtype != 1) return buildNxdomainResponse(query)

            val questionSection = query.copyOfRange(12, qBuf.position())
            val buf = ByteBuffer.allocate(12 + questionSection.size + 16)
            buf.putShort(id.toShort())
            buf.putShort(0x8180.toShort())
            buf.putShort(1); buf.putShort(1); buf.putShort(0); buf.putShort(0)
            buf.put(questionSection)
            buf.putShort(0xC00C.toShort())
            buf.putShort(1); buf.putShort(1)
            buf.putInt(10); buf.putShort(4); buf.putInt(0)
            buf.array()
        } catch (e: Exception) {
            buildNxdomainResponse(query)
        }
    }

    fun buildBlockedAaaaResponse(query: ByteArray): ByteArray {
        return try {
            val qBuf = ByteBuffer.wrap(query)
            val id = qBuf.short.toInt() and 0xFFFF
            qBuf.position(12)
            while (qBuf.hasRemaining()) {
                val len = qBuf.get().toInt() and 0xFF
                if (len == 0) break
                if (len and 0xC0 == 0xC0) { qBuf.get(); break }
                if (qBuf.remaining() < len) return buildNxdomainResponse(query)
                qBuf.position(qBuf.position() + len)
            }
            if (qBuf.remaining() < 4) return buildNxdomainResponse(query)
            val qtype = qBuf.short.toInt() and 0xFFFF
            if (qtype != 28) return buildNxdomainResponse(query)

            val questionSection = query.copyOfRange(12, qBuf.position())
            val buf = ByteBuffer.allocate(12 + questionSection.size + 28)
            buf.putShort(id.toShort())
            buf.putShort(0x8180.toShort())
            buf.putShort(1); buf.putShort(1); buf.putShort(0); buf.putShort(0)
            buf.put(questionSection)
            buf.putShort(0xC00C.toShort())
            buf.putShort(28); buf.putShort(1)
            buf.putInt(10); buf.putShort(16)
            buf.put(ByteArray(16)) // :: null IPv6
            buf.array()
        } catch (e: Exception) {
            buildNxdomainResponse(query)
        }
    }

    fun buildBlockedResponse(query: DnsQuery, rawQuery: ByteArray): ByteArray {
        return when {
            query.isARecord -> buildBlockedAResponse(rawQuery)
            query.isAAAARecord -> buildBlockedAaaaResponse(rawQuery)
            else -> buildNxdomainResponse(rawQuery)
        }
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

data class IpTcpPacket(
    val version: Int,
    val srcIp: ByteArray,
    val dstIp: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val seqNumber: Long,
    val ackNumber: Long,
    val flags: Int,
    val payload: ByteArray,
    val rawPacket: ByteArray
)

object IpPacketParser {

    fun parseUdpPacket(raw: ByteArray): IpUdpPacket? {
        return try {
            val buf = ByteBuffer.wrap(raw)
            val versionIhl = buf.get().toInt() and 0xFF
            val version = versionIhl shr 4
            val ihl = (versionIhl and 0x0F) * 4

            if (version == 4) {
                if (raw.size < ihl + 8) return null
                buf.position(9)
                val protocol = buf.get().toInt() and 0xFF
                if (protocol != 17) return null

                buf.position(12)
                val srcIp = ByteArray(4); buf.get(srcIp)
                val dstIp = ByteArray(4); buf.get(dstIp)
                buf.position(ihl)
                val srcPort = buf.short.toInt() and 0xFFFF
                val dstPort = buf.short.toInt() and 0xFFFF
                val udpLen = (buf.short.toInt() and 0xFFFF) - 8
                buf.short
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

            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun buildUdpResponse(request: IpUdpPacket, dnsResponse: ByteArray): ByteArray {
        return if (request.version == 4) {
            buildIpv4UdpPacket(request.dstIp, request.srcIp, request.dstPort, request.srcPort, dnsResponse)
        } else {
            buildIpv6UdpPacket(request.dstIp, request.srcIp, request.dstPort, request.srcPort, dnsResponse)
        }
    }

    private fun buildIpv4UdpPacket(
        srcIp: ByteArray, dstIp: ByteArray,
        srcPort: Int, dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val udpLen = 8 + payload.size
        val ipLen = 20 + udpLen
        val buf = ByteBuffer.allocate(ipLen)
        buf.put(0x45.toByte())
        buf.put(0x00.toByte())
        buf.putShort(ipLen.toShort())
        buf.putShort(0)
        buf.putShort(0x4000.toShort())
        buf.put(64.toByte())
        buf.put(17.toByte())
        buf.putShort(0)
        buf.put(srcIp); buf.put(dstIp)
        val packet = buf.array()
        val checksum = ipv4Checksum(packet, 0, 20)
        packet[10] = (checksum shr 8).toByte()
        packet[11] = checksum.toByte()
        buf.putShort(srcPort.toShort()); buf.putShort(dstPort.toShort())
        buf.putShort(udpLen.toShort()); buf.putShort(0)
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
        buf.putInt(0x60000000.toInt())
        buf.putShort(udpLen.toShort())
        buf.put(17.toByte()); buf.put(64.toByte())
        buf.put(srcIp); buf.put(dstIp)
        buf.putShort(srcPort.toShort()); buf.putShort(dstPort.toShort())
        buf.putShort(udpLen.toShort()); buf.putShort(0)
        buf.put(payload)
        val packet = buf.array()

        // RFC 8200: Calculate mandatory IPv6 UDP pseudo-header checksum
        val checksum = ipv6UdpChecksum(srcIp, dstIp, udpLen, packet, 40)
        packet[46] = (checksum shr 8).toByte()
        packet[47] = checksum.toByte()

        return packet
    }

    private fun ipv6UdpChecksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        udpLen: Int,
        packet: ByteArray,
        udpOffset: Int
    ): Int {
        var sum = 0L

        // 1. Source IPv6 (16 bytes = 8 shorts)
        for (i in 0 until 16 step 2) {
            sum += ((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)
        }

        // 2. Destination IPv6 (16 bytes = 8 shorts)
        for (i in 0 until 16 step 2) {
            sum += ((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)
        }

        // 3. UDP Length (32-bit: 2 shorts)
        sum += (udpLen shr 16) and 0xFFFF
        sum += udpLen and 0xFFFF

        // 4. Next Header (32-bit: 3 zero bytes + protocol 17)
        sum += 17

        // 5. UDP Header & Payload (from udpOffset)
        var i = udpOffset
        val end = udpOffset + udpLen
        while (i < end - 1) {
            // Skip the checksum field itself (offset 6 and 7 within UDP header)
            if (i == udpOffset + 6) {
                i += 2
                continue
            }
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            i += 2
        }
        // Pad odd byte if any
        if (i < end) {
            sum += (packet[i].toInt() and 0xFF) shl 8
        }

        // Fold 32-bit sum to 16 bits
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        val checksum = (sum.inv() and 0xFFFF).toInt()
        return if (checksum == 0) 0xFFFF else checksum
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

    fun parseTcpPacket(raw: ByteArray): IpTcpPacket? {
        return try {
            if (raw.size < 20) return null
            val buf = ByteBuffer.wrap(raw)
            val versionIhl = buf.get().toInt() and 0xFF
            val version = versionIhl shr 4
            val ihl = (versionIhl and 0x0F) * 4

            if (version == 4) {
                if (raw.size < ihl + 20) return null
                buf.position(9)
                val protocol = buf.get().toInt() and 0xFF
                if (protocol != 6) return null // 6 = TCP

                buf.position(12)
                val srcIp = ByteArray(4); buf.get(srcIp)
                val dstIp = ByteArray(4); buf.get(dstIp)

                buf.position(ihl)
                val srcPort = buf.short.toInt() and 0xFFFF
                val dstPort = buf.short.toInt() and 0xFFFF
                val seqNumber = buf.int.toLong() and 0xFFFFFFFFL
                val ackNumber = buf.int.toLong() and 0xFFFFFFFFL
                val dataOffsetAndReserved = buf.get().toInt() and 0xFF
                val tcpHeaderLen = ((dataOffsetAndReserved shr 4) and 0x0F) * 4
                val flags = buf.get().toInt() and 0xFF

                val payloadOffset = ihl + tcpHeaderLen
                val payloadLen = (raw.size - payloadOffset).coerceAtLeast(0)
                val payload = if (payloadLen > 0 && payloadOffset + payloadLen <= raw.size) {
                    raw.copyOfRange(payloadOffset, payloadOffset + payloadLen)
                } else {
                    ByteArray(0)
                }

                IpTcpPacket(4, srcIp, dstIp, srcPort, dstPort, seqNumber, ackNumber, flags, payload, raw)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun buildTcpRstResponse(request: IpTcpPacket): ByteArray {
        val hasAck = (request.flags and 0x10) != 0
        val rstSeq = if (hasAck) request.ackNumber else 0L
        val rstAck = if (hasAck) 0L else (request.seqNumber + 1L) and 0xFFFFFFFFL
        val rstFlags = if (hasAck) 0x04 else 0x14 // RST (0x04) or RST|ACK (0x14)

        val totalLen = 40 // 20 IP + 20 TCP
        val buf = ByteBuffer.allocate(totalLen)

        // IPv4 Header
        buf.put(0x45.toByte())
        buf.put(0x00.toByte())
        buf.putShort(totalLen.toShort())
        buf.putShort(0)
        buf.putShort(0x4000.toShort()) // Don't fragment
        buf.put(64.toByte()) // TTL
        buf.put(6.toByte()) // Protocol TCP
        buf.putShort(0) // Checksum placeholder
        buf.put(request.dstIp) // Src IP is original Dst IP
        buf.put(request.srcIp) // Dst IP is original Src IP

        // IP Checksum
        val ipChecksum = ipv4Checksum(buf.array(), 0, 20)
        buf.array()[10] = (ipChecksum shr 8).toByte()
        buf.array()[11] = ipChecksum.toByte()

        // TCP Header (at offset 20)
        buf.position(20)
        buf.putShort(request.dstPort.toShort()) // Src port
        buf.putShort(request.srcPort.toShort()) // Dst port
        buf.putInt(rstSeq.toInt())
        buf.putInt(rstAck.toInt())
        buf.put((5 shl 4).toByte()) // Data offset: 5 32-bit words (20 bytes)
        buf.put(rstFlags.toByte())
        buf.putShort(0) // Window size: 0
        buf.putShort(0) // TCP checksum placeholder
        buf.putShort(0) // Urgent pointer: 0

        // TCP Checksum with pseudo-header
        val tcpChecksum = tcpChecksum(request.dstIp, request.srcIp, 20, buf.array(), 20)
        buf.array()[36] = (tcpChecksum shr 8).toByte()
        buf.array()[37] = tcpChecksum.toByte()

        return buf.array()
    }

    private fun tcpChecksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        tcpLen: Int,
        packet: ByteArray,
        tcpOffset: Int
    ): Int {
        var sum = 0L

        // IPv4 Pseudo Header:
        sum += ((srcIp[0].toInt() and 0xFF) shl 8) or (srcIp[1].toInt() and 0xFF)
        sum += ((srcIp[2].toInt() and 0xFF) shl 8) or (srcIp[3].toInt() and 0xFF)
        sum += ((dstIp[0].toInt() and 0xFF) shl 8) or (dstIp[1].toInt() and 0xFF)
        sum += ((dstIp[2].toInt() and 0xFF) shl 8) or (dstIp[3].toInt() and 0xFF)
        sum += 6 // Protocol 6
        sum += tcpLen

        var i = tcpOffset
        val end = tcpOffset + tcpLen
        while (i < end - 1) {
            if (i == tcpOffset + 16) { // skip checksum field at offset 16 in TCP header
                i += 2
                continue
            }
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) {
            sum += (packet[i].toInt() and 0xFF) shl 8
        }

        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        val checksum = (sum.inv() and 0xFFFF).toInt()
        return if (checksum == 0) 0xFFFF else checksum
    }
}
