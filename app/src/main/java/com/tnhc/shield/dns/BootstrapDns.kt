package com.tnhc.shield.dns

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Hardcoded bootstrap DNS resolver for known DoH providers.
 * Eliminates bootstrapping loops when establishing TLS connections to DoH endpoints
 * before system DNS is available or when VPN intercepts port 53.
 */
object BootstrapDns : Dns {

    private val staticBootstrapMap = mapOf(
        "cloudflare-dns.com" to listOf(
            "1.1.1.1",
            "1.0.0.1",
            "2606:4700:4700::1111",
            "2606:4700:4700::1001"
        ),
        "dns.google" to listOf(
            "8.8.8.8",
            "8.8.4.4",
            "2001:4860:4860::8888",
            "2001:4860:4860::8844"
        ),
        "dns.quad9.net" to listOf(
            "9.9.9.9",
            "149.112.112.112",
            "2620:fe::fe",
            "2620:fe::9"
        ),
        "dns.adguard-dns.com" to listOf(
            "94.140.14.14",
            "94.140.15.15",
            "2a10:50c0::ad1:ff",
            "2a10:50c0::ad2:ff"
        ),
        "dns.nextdns.io" to listOf(
            "45.90.28.0",
            "45.90.30.0",
            "2a07:a8c0::",
            "2a07:a8c1::"
        )
    )

    override fun lookup(hostname: String): List<InetAddress> {
        val lowerHost = hostname.lowercase()
        val staticIps = staticBootstrapMap[lowerHost]
        if (staticIps != null) {
            return staticIps.mapNotNull { ip ->
                try {
                    InetAddress.getByAddress(hostname, InetAddress.getByName(ip).address)
                } catch (_: Exception) {
                    null
                }
            }.ifEmpty {
                Dns.SYSTEM.lookup(hostname)
            }
        }
        return Dns.SYSTEM.lookup(hostname)
    }
}
