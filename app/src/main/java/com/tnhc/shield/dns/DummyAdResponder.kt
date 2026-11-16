package com.tnhc.shield.dns

object DummyAdResponder {

    // 43-byte valid RFC compliant 1x1 transparent GIF89a
    val TRANSPARENT_1X1_GIF = byteArrayOf(
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, // GIF89a
        0x01, 0x00, 0x01, 0x00,             // 1x1 width/height
        0x80.toByte(), 0x00, 0x00,          // Global color table flag
        0x00, 0x00, 0x00,                   // Black
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // White
        0x21, 0xF9.toByte(), 0x04,          // Graphic Control Extension
        0x01, 0x00, 0x00, 0x00, 0x00,       // Transparent index 0
        0x2C,                               // Image Descriptor
        0x00, 0x00, 0x00, 0x00,             // 0,0
        0x01, 0x00, 0x01, 0x00,             // 1x1
        0x00,                               // No local color table
        0x02, 0x02, 0x44, 0x01, 0x00,       // LZW min code size 2 + data
        0x3B                                // Trailer ';'
    )

    val EMPTY_JSON = "{}".toByteArray(Charsets.UTF_8)
    val EMPTY_JS = "/* Shield blocked */".toByteArray(Charsets.UTF_8)

    fun buildHttpResponse(body: ByteArray, contentType: String): ByteArray {
        val header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: $contentType\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Connection: close\r\n\r\n"
        val headerBytes = header.toByteArray(Charsets.US_ASCII)
        val full = ByteArray(headerBytes.size + body.size)
        System.arraycopy(headerBytes, 0, full, 0, headerBytes.size)
        System.arraycopy(body, 0, full, headerBytes.size, body.size)
        return full
    }
}
