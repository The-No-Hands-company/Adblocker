package com.tnhc.shield.dns

import java.nio.ByteBuffer

/**
 * High-performance TLS ClientHello Server Name Indication (SNI) parser.
 * Inspects initial TLS handshake payloads on port 443 to extract destination hostnames
 * for ad SDKs utilizing hardcoded direct IP addresses to bypass standard DNS interception.
 */
object SniParser {

    /**
     * Parses a TLS ClientHello packet payload (starting at the TLS record layer)
     * and extracts the Server Name Indication (SNI) hostname if present.
     *
     * Returns null if the packet is not a valid TLS ClientHello or contains no SNI extension.
     */
    fun parseSni(data: ByteArray): String? {
        return try {
            if (data.size < 44) return null
            val buf = ByteBuffer.wrap(data)

            // 1. TLS Record Header (5 bytes)
            val contentType = buf.get().toInt() and 0xFF
            if (contentType != 0x16) return null // 0x16 = Handshake record

            // Version (2 bytes)
            buf.short
            val recordLength = buf.short.toInt() and 0xFFFF
            if (buf.remaining() < recordLength) return null

            // 2. Handshake Header (4 bytes)
            val handshakeType = buf.get().toInt() and 0xFF
            if (handshakeType != 0x01) return null // 0x01 = ClientHello

            // 3-byte handshake length
            val len1 = buf.get().toInt() and 0xFF
            val len2 = buf.get().toInt() and 0xFF
            val len3 = buf.get().toInt() and 0xFF
            val handshakeLength = (len1 shl 16) or (len2 shl 8) or len3

            if (buf.remaining() < handshakeLength) return null

            // Client Version (2 bytes)
            buf.short

            // Random (32 bytes)
            if (buf.remaining() < 32) return null
            buf.position(buf.position() + 32)

            // Session ID
            if (!buf.hasRemaining()) return null
            val sessionIdLen = buf.get().toInt() and 0xFF
            if (buf.remaining() < sessionIdLen) return null
            buf.position(buf.position() + sessionIdLen)

            // Cipher Suites
            if (buf.remaining() < 2) return null
            val cipherSuitesLen = buf.short.toInt() and 0xFFFF
            if (buf.remaining() < cipherSuitesLen) return null
            buf.position(buf.position() + cipherSuitesLen)

            // Compression Methods
            if (!buf.hasRemaining()) return null
            val compressionMethodsLen = buf.get().toInt() and 0xFF
            if (buf.remaining() < compressionMethodsLen) return null
            buf.position(buf.position() + compressionMethodsLen)

            // Extensions Length
            if (buf.remaining() < 2) return null
            val extensionsLen = buf.short.toInt() and 0xFFFF
            if (buf.remaining() < extensionsLen) return null

            val extensionsEnd = buf.position() + extensionsLen

            // Iterate over TLS Extensions
            while (buf.position() + 4 <= extensionsEnd) {
                val extType = buf.short.toInt() and 0xFFFF
                val extLen = buf.short.toInt() and 0xFFFF

                if (buf.position() + extLen > extensionsEnd) return null

                if (extType == 0x0000) { // Extension: server_name (SNI)
                    if (extLen < 5) return null
                    val serverNameListLen = buf.short.toInt() and 0xFFFF
                    if (serverNameListLen < 3) return null

                    val nameType = buf.get().toInt() and 0xFF
                    val hostNameLen = buf.short.toInt() and 0xFFFF
                    if (nameType == 0x00 && buf.remaining() >= hostNameLen) { // 0x00 = host_name
                        val hostBytes = ByteArray(hostNameLen)
                        buf.get(hostBytes)
                        return String(hostBytes, Charsets.US_ASCII).lowercase()
                    }
                } else {
                    buf.position(buf.position() + extLen)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
