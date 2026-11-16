package com.tnhc.shield.filter

import org.junit.Assert.*
import org.junit.Test

class IpBlocklistEngineTest {

    private val engine = IpBlocklistEngine()

    @Test
    fun testExactIpMatches() {
        assertTrue(engine.isBlocked("34.192.122.188")) // Unity ads auction
        assertTrue(engine.isBlocked("52.206.163.4"))   // AppLovin rt
        assertFalse(engine.isBlocked("1.1.1.1"))
    }

    @Test
    fun testCidrMatchingHelper() {
        val cidr = IpBlocklistEngine.parseIpv4Cidr("192.168.1.0/24")
        val ipParts = listOf(192, 168, 1, 50)
        val ipInt = (ipParts[0] shl 24) or (ipParts[1] shl 16) or (ipParts[2] shl 8) or ipParts[3]
        assertTrue(cidr.matches(ipInt))

        val ipPartsOther = listOf(192, 168, 2, 50)
        val ipIntOther = (ipPartsOther[0] shl 24) or (ipPartsOther[1] shl 16) or (ipPartsOther[2] shl 8) or ipPartsOther[3]
        assertFalse(cidr.matches(ipIntOther))
    }
}
