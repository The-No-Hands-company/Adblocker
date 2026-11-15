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
    fun testCidrSubnetMatches() {
        // "185.199.108.0/22" covers 185.199.108.0 to 185.199.111.255
        assertTrue(engine.isBlocked("185.199.108.15"))
        assertTrue(engine.isBlocked("185.199.111.200"))
        assertFalse(engine.isBlocked("185.199.112.1"))
    }
}
