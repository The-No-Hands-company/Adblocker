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
