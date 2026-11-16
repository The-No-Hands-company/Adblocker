package com.tnhc.shield.filter

import com.tnhc.shield.data.db.entities.CustomRuleEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FilterEngineTest {

    private lateinit var filterEngine: FilterEngine

    @Before
    fun setup() {
        filterEngine = FilterEngine()
    }

    @Test
    fun testBuiltInGameAdDomainsAlwaysBlocked() {
        assertTrue(filterEngine.isBlocked("unityads.unity3d.com"))
        assertTrue(filterEngine.isBlocked("rt.applovin.com"))
        assertTrue(filterEngine.isBlocked("ads.ironsrc.com"))
        assertFalse(filterEngine.isBlocked("google.com"))
    }

    @Test
    fun testCustomRulesPriority() {
        // Built-in blocked domain
        assertTrue(filterEngine.isBlocked("unityads.unity3d.com"))

        // Add user whitelist for it
        filterEngine.loadCustomRules(
            listOf(
                CustomRuleEntity(domain = "unityads.unity3d.com", isWhitelist = true)
            )
        )

        // Whitelist must override blocklist
        assertFalse(filterEngine.isBlocked("unityads.unity3d.com"))
        assertTrue(filterEngine.isWhitelisted("unityads.unity3d.com"))

        // Add user blacklist for benign domain
        filterEngine.addUserRule("custom-tracker.com", block = true)
        assertTrue(filterEngine.isBlocked("custom-tracker.com"))
    }

    @Test
    fun testSubdomainSuffixMatching() {
        val parseResult = FilterListParser.ParseResult(
            blockedDomains = setOf("doubleclick.net"),
            whitelistedDomains = emptySet()
        )
        filterEngine.loadRules(listOf(parseResult))

        assertTrue(filterEngine.isBlocked("doubleclick.net"))
        assertTrue(filterEngine.isBlocked("ad.doubleclick.net"))
        assertTrue(filterEngine.isBlocked("sub.ad.doubleclick.net"))
        assertFalse(filterEngine.isBlocked("notdoubleclick.net"))
    }

    @Test
    fun testEssentialInfrastructureNeverBlocked() {
        val parseResult = FilterListParser.ParseResult(
            blockedDomains = setOf("unity3d.com", "googleapis.com"),
            whitelistedDomains = emptySet()
        )
        filterEngine.loadRules(listOf(parseResult))

        // Essential game engine / auth domains must remain unblocked for games to start
        assertFalse(filterEngine.isBlocked("cloud.unity3d.com"))
        assertFalse(filterEngine.isBlocked("play.googleapis.com"))
        assertTrue(filterEngine.isWhitelisted("cloud.unity3d.com"))

        // While ad domains under unity3d are still blocked
        assertTrue(filterEngine.isBlocked("unityads.unity3d.com"))
    }
}
