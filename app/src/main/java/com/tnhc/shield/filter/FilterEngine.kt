package com.tnhc.shield.filter

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class FilterEngine {

    private val lock = ReentrantReadWriteLock()

    // Exact domain matches: "ads.example.com"
    private var blockedExact = HashSet<String>(64_000)

    // Hardcoded SDK domains — always blocked, survive filter list reloads.
    // Covers major game/in-app ad SDKs that generic lists sometimes miss.
    private val builtInBlockedExact: Set<String> = setOf(
        // Unity Ads / LevelPlay (IronSource)
        "unityads.unity3d.com",
        "auction.unityads.unity3d.com",
        "config.unityads.unity3d.com",
        "gameads.unity3d.com",
        "ads.unity3d.com",
        "static.ads.unity3d.com",
        "video.unityads.unity3d.com",
        "impact.unityads.unity3d.com",
        "ads.ironsrc.com",
        "delivery.ironsrc.com",
        "outcome.ironsrc.com",
        "impression.ironsrc.com",
        "sis.ironsrc.com",
        "api4.bat.bing.com",
        // AppLovin / MAX
        "rt.applovin.com",
        "d.applovin.com",
        "ads.applovin.com",
        "f.applovin.com",
        "track.applovin.com",
        "mrtn.applovin.com",
        "mobileperf.applovin.com",
        "a.applovin.com",
        "postbacks.applovin.com",
        // Vungle / Liftoff
        "api.vungle.com",
        "ads.vungle.com",
        "api.liftoff.io",
        "events.liftoff.io",
        // Chartboost
        "live.chartboost.com",
        "ads.chartboost.com",
        // AdColony
        "adc3-launch.adcolony.com",
        "ads.adcolony.com",
        "events.adcolony.com",
        // Tapjoy
        "ads.tapjoy.com",
        "api.tapjoy.com",
        "reporting.tapjoy.com",
        // InMobi
        "api.inmobi.com",
        "adincdn.inmobi.com",
        "ads.inmobi.com",
        // Pangle (TikTok for Business)
        "api16-pa.tiktokv.com",
        "log.tiktokv.com",
        "api3-pa.com",
        // MoPub (retired but still deployed in older games)
        "ads.mopub.com",
        "ads.mopub.com",
        // Moloco
        "ads.moloco.com",
        "metrics.moloco.com",
        // Digital Turbine / Fyber
        "ads.fyber.com",
        "reporting.fyber.com",
        // Smaato
        "prebid.smaato.net",
        "ads.smaato.net"
    )

    // Domain suffix matches: entries like "example.com" block "ads.example.com", "sub.ads.example.com", etc.
    private var blockedSuffix = HashSet<String>(32_000)

    // Whitelisted domains (never blocked)
    private var whitelistedExact = HashSet<String>(4_000)
    private var whitelistedSuffix = HashSet<String>(2_000)

    // Custom user rules
    private var userBlockedExact = HashSet<String>(256)
    private var userWhitelistedExact = HashSet<String>(256)

    var totalRules: Int = 0
        private set

    fun loadRules(results: List<FilterListParser.ParseResult>) {
        val newBlocked = HashSet<String>(64_000)
        val newWhitelisted = HashSet<String>(4_000)

        results.forEach { result ->
            newBlocked.addAll(result.blockedDomains)
            newWhitelisted.addAll(result.whitelistedDomains)
        }

        // Split into exact and suffix sets
        // Heuristic: if a domain has 2+ labels and no wildcard, treat as exact
        // Domains with 1 label (e.g. "com") are suffix rules
        val newBlockedExact = HashSet<String>(newBlocked.size)
        val newBlockedSuffix = HashSet<String>(newBlocked.size / 4)

        newBlocked.forEach { domain ->
            if (domain.count { it == '.' } >= 1) newBlockedExact.add(domain)
            else newBlockedSuffix.add(domain)
        }

        lock.write {
            blockedExact = newBlockedExact
            blockedSuffix = newBlockedSuffix
            whitelistedExact = HashSet(newWhitelisted)
            whitelistedSuffix = HashSet()
            totalRules = newBlockedExact.size + newBlockedSuffix.size + builtInBlockedExact.size
        }
    }

    fun isBlocked(domain: String): Boolean {
        val d = domain.lowercase().trimEnd('.')
        lock.read {
            if (userWhitelistedExact.contains(d)) return false
            if (whitelistedExact.contains(d)) return false
            if (builtInBlockedExact.contains(d)) return true
            if (userBlockedExact.contains(d)) return true

            // Check exact match
            if (blockedExact.contains(d)) return true

            // Check suffix matches: for "ads.sub.example.com" check "sub.example.com", "example.com", "com"
            var dotIndex = d.indexOf('.')
            while (dotIndex != -1) {
                val suffix = d.substring(dotIndex + 1)
                if (whitelistedExact.contains(suffix)) return false
                if (blockedExact.contains(suffix)) return true
                if (blockedSuffix.contains(suffix)) return true
                dotIndex = d.indexOf('.', dotIndex + 1)
            }
        }
        return false
    }

    fun isWhitelisted(domain: String): Boolean {
        val d = domain.lowercase().trimEnd('.')
        lock.read {
            if (userWhitelistedExact.contains(d)) return true
            if (whitelistedExact.contains(d)) return true
            var dotIndex = d.indexOf('.')
            while (dotIndex != -1) {
                val suffix = d.substring(dotIndex + 1)
                if (whitelistedExact.contains(suffix)) return true
                dotIndex = d.indexOf('.', dotIndex + 1)
            }
        }
        return false
    }

    fun addUserRule(domain: String, block: Boolean) {
        lock.write {
            if (block) userBlockedExact.add(domain.lowercase())
            else userWhitelistedExact.add(domain.lowercase())
        }
    }

    fun removeUserRule(domain: String) {
        lock.write {
            userBlockedExact.remove(domain.lowercase())
            userWhitelistedExact.remove(domain.lowercase())
        }
    }

    fun getUserBlockedDomains(): Set<String> = lock.read { userBlockedExact.toSet() }
    fun getUserWhitelistedDomains(): Set<String> = lock.read { userWhitelistedExact.toSet() }

    fun clear() {
        lock.write {
            blockedExact.clear()
            blockedSuffix.clear()
            whitelistedExact.clear()
            whitelistedSuffix.clear()
            totalRules = 0
        }
    }
}
