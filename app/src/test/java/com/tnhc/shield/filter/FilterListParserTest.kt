package com.tnhc.shield.filter

import org.junit.Assert.*
import org.junit.Test

class FilterListParserTest {

    @Test
    fun testParseHostsFormat() {
        val hostsContent = """
            # This is a comment
            127.0.0.1 localhost
            0.0.0.0 ads.example.com
            0.0.0.0 tracker.analytics.org # inline comment
            ::1 localhost
        """.trimIndent()

        val result = FilterListParser.parse(hostsContent, FilterFormat.HOSTS)
        assertTrue(result.blockedDomains.contains("ads.example.com"))
        assertTrue(result.blockedDomains.contains("tracker.analytics.org"))
        assertFalse(result.blockedDomains.contains("localhost"))
    }

    @Test
    fun testParseDomainListFormat() {
        val domainList = """
            # Top ad domains
            ads.unity3d.com
            rt.applovin.com
            ! Whitelist
            @@||whitelist.com^
        """.trimIndent()

        val result = FilterListParser.parse(domainList, FilterFormat.DOMAIN_LIST)
        assertTrue(result.blockedDomains.contains("ads.unity3d.com"))
        assertTrue(result.blockedDomains.contains("rt.applovin.com"))
    }
}
