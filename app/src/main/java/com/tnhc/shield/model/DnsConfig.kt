package com.tnhc.shield.model

data class DnsConfig(
    val serverUrl: String = "https://cloudflare-dns.com/dns-query",
    val serverName: String = "Cloudflare",
    val blockMode: BlockMode = BlockMode.NXDOMAIN,
    val cacheEnabled: Boolean = true,
    val logQueries: Boolean = false
)

enum class BlockMode(val displayName: String) {
    NXDOMAIN("NXDOMAIN (recommended)"),
    NULL_IP("0.0.0.0"),
    REFUSED("REFUSED")
}
