package com.tnhc.shield.filter

import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * IP-level ad blocking engine.
 * Matches IP addresses against CIDR prefixes and hardcoded edge clusters used by
 * Unity Ads, IronSource/LevelPlay, and AppLovin SDK builds that bypass DNS via hardcoded IPs.
 */
class IpBlocklistEngine {

    data class Ipv4Cidr(val network: Int, val mask: Int) {
        fun matches(ip: Int): Boolean = (ip and mask) == network
    }

    // Do NOT include shared Anycast CDN ranges (Fastly, Cloudflare, AWS CloudFront)
    // as doing so would block legitimate apps and games sharing those cloud networks.
    private val staticIpv4Cidrs: List<Ipv4Cidr> = emptyList()

    private val staticExactIpv4Strings = setOf(
        "34.192.122.188", // unityads auction
        "54.88.225.234",  // unityads config
        "52.206.163.4",   // applovin rt
        "54.210.229.138", // ironsrc delivery
        "52.7.202.94"     // vungle ads
    )

    private val staticExactIpv4Ints: Set<Int> = staticExactIpv4Strings.map { ip ->
        val parts = ip.split('.').map { it.toInt() }
        (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
    }.toSet()

    val routedExactIps: Set<String> get() = staticExactIpv4Strings

    fun isBlocked(ipAddress: String): Boolean {
        if (staticExactIpv4Strings.contains(ipAddress)) return true
        return try {
            val addr = InetAddress.getByName(ipAddress)
            isBlocked(addr.address)
        } catch (_: Exception) {
            false
        }
    }

    fun isBlocked(ipBytes: ByteArray): Boolean {
        if (ipBytes.size == 4) {
            val ipInt = ByteBuffer.wrap(ipBytes).int
            if (staticExactIpv4Ints.contains(ipInt)) return true
            for (cidr in staticIpv4Cidrs) {
                if (cidr.matches(ipInt)) return true
            }
        }
        return false
    }

    companion object {
        fun parseIpv4Cidr(cidr: String): Ipv4Cidr {
            val parts = cidr.split('/')
            val ipParts = parts[0].split('.').map { it.toInt() }
            val prefix = if (parts.size > 1) parts[1].toInt() else 32

            val ipInt = (ipParts[0] shl 24) or (ipParts[1] shl 16) or (ipParts[2] shl 8) or ipParts[3]
            val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
            val network = ipInt and mask

            return Ipv4Cidr(network, mask)
        }
    }
}
