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

    private val staticIpv4Cidrs: List<Ipv4Cidr> = listOf(
        // Unity Ads & Video Delivery Edge Clusters (Akamai / Fastly / Dedicated CDN ranges)
        parseIpv4Cidr("185.199.108.0/22"),
        parseIpv4Cidr("192.241.128.0/19"),
        parseIpv4Cidr("162.243.0.0/18"),
        parseIpv4Cidr("151.101.0.0/16"),

        // AppLovin / MAX Edge Ad Delivery Clusters
        parseIpv4Cidr("54.240.128.0/18"),
        parseIpv4Cidr("52.216.0.0/15"),
        parseIpv4Cidr("34.224.0.0/12"),
        parseIpv4Cidr("35.160.0.0/13"),

        // IronSource / LevelPlay Impression & Auction Clusters
        parseIpv4Cidr("52.84.0.0/15"),
        parseIpv4Cidr("13.32.0.0/15"),
        parseIpv4Cidr("18.64.0.0/14"),
        parseIpv4Cidr("65.8.0.0/15"),

        // InMobi / Vungle Ad Gateways
        parseIpv4Cidr("104.16.0.0/13"),
        parseIpv4Cidr("104.24.0.0/14")
    )

    private val staticExactIpv4 = hashSetOf(
        "34.192.122.188", // unityads auction
        "54.88.225.234",  // unityads config
        "52.206.163.4",   // applovin rt
        "54.210.229.138", // ironsrc delivery
        "52.7.202.94"     // vungle ads
    )

    fun isBlocked(ipAddress: String): Boolean {
        if (staticExactIpv4.contains(ipAddress)) return true
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
