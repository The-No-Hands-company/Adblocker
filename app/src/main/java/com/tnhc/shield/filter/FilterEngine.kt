package com.tnhc.shield.filter

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class FilterEngine {

    private val lock = ReentrantReadWriteLock()

    // Exact domain matches: "ads.example.com"
    private var blockedExact = HashSet<String>(64_000)

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
            totalRules = newBlockedExact.size + newBlockedSuffix.size
        }
    }

    fun isBlocked(domain: String): Boolean {
        val d = domain.lowercase().trimEnd('.')
        lock.read {
            if (userWhitelistedExact.contains(d)) return false
            if (whitelistedExact.contains(d)) return false
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
