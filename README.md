# Shield — TNHC Adblocker

> **Your Digital Immune System** — the best free adblocker for Android, by The No Hands Company.

## What it does

Shield blocks ads, trackers, malware, and annoyances system-wide on Android — across **every app**, not just your browser — using a local VPN DNS filter. No root required. No cloud dependency. Zero telemetry.

## The Android Adblocker Landscape: Weaknesses vs. Shield Superiority

To dominate the Android ad-blocking space without asking for a dime, Shield targets and eliminates every vulnerability, paywall, and compromise found in other solutions:

| Competitor | Their Critical Weaknesses | How Shield Dominates |
| :--- | :--- | :--- |
| **AdGuard for Android** | • Aggressive monetization & paywalled features (requires €20+/yr for HTTPS & app control).<br>• Heavy battery drain from proxying all TCP traffic through a full userspace stack.<br>• Heavy, closed-source background services. | • **100% Free Forever**: Zero paywalls, zero subscriptions.<br>• **Ultra-Low Battery Footprint**: Intercepts only DNS (port 53) and targeted TLS handshakes; regular payload traffic streams at raw hardware speeds.<br>• Pure, lean Kotlin. |
| **Blokada (v5 / v6)** | • Abandoned its open-source roots to push "Blokada Cloud" subscriptions.<br>• High memory consumption on Android when processing large blocklists.<br>• Clunky UX with recurring background process terminations. | • **O(1) HashSet Engine**: Sub-millisecond lookups across hundreds of thousands of rules with low memory footprint.<br>• Clean Material You 3 UI with native Quick Settings Tile. |
| **DNS66 / personalDNSfilter** | • Archaic, neglected UI from the Android 6 era.<br>• IPv6 Leaks: Carrier networks (T-Mobile, Verizon) prioritize IPv6 DNS and bypass the VPN entirely.<br>• Deadlock crashes: Upstream DoH resolver hangs when resolving its own bootstrap IP. | • **Dual-Stack IPv6 Interception**: Bound to `fd00::1/128` with RFC 8200 checksums to eliminate mobile carrier leaks.<br>• **Bootstrap DNS**: Hardcoded static IPs for Cloudflare, Google, Quad9, AdGuard, and NextDNS to eliminate deadlocks. |
| **Android Native Private DNS (DoT)** | • Global toggle only: Cannot exclude broken apps (banking, work apps).<br>• No real-time query visibility or custom rules.<br>• Completely bypassed by in-app ads using hardcoded IP addresses. | • **Granular Split-Tunneling**: Per-app exclusion.<br>• **Live Query Log**: 1-tap whitelist button for broken apps.<br>• **Custom Rules**: Instant overrides without editing hosts files. |
| **All DNS-Only Blockers** | • Hardcoded IP Ads: Ad SDKs like Unity Ads, AppLovin, and IronSource embed raw IP ranges directly into game binaries, completely bypassing DNS resolution. | • **IP Blocklist & SNI Parser**: Includes CIDR bitmask engine (`IpBlocklistEngine.kt`), direct TCP RST rejection, and TLS ClientHello inspection (`SniParser.kt`) to intercept and kill hardcoded ad clusters in < 1ms. |

## Default Filter Lists (12 included)

1. **OISD Full** — The internet's most comprehensive unified blocklist
2. **AdGuard Base** — AdGuard's optimized Android filter
3. **EasyList** — Classic ad blocking (20M+ users)
4. **EasyPrivacy** — Full tracker removal
5. **Steven Black Hosts** — Combined adware + malware hosts
6. **AdGuard Tracking Protection** — Analytics & pixels
7. **AdGuard Mobile Ads** — In-app ad networks
8. **Goodbye Ads (Mobile)** — Aggressive mobile filter
9. **Malware Domains** — Known malware distribution sites
10. **Fanboy Annoyances** — Cookie banners, pop-ups
11. **HaGeZi Pro** — Professional-grade DNS blocklist
12. **AdGuard Social Media** — Social tracking widgets (optional)

## Architecture

```
Android VPN Service (ShieldVpnService)
    │
    ├── TUN Interface (10.0.0.2)
    │   └── Intercepts DNS queries from all apps
    │
    ├── DNS Engine (DnsEngine)
    │   ├── FilterEngine → check against 300K+ domain blocklist (O(1) HashSet)
    │   ├── DNS Cache → TTL-aware response cache (up to 2048 entries)
    │   └── DoH Client → Cloudflare/Google/Quad9/AdGuard/NextDNS
    │
    ├── Filter Engine (FilterEngine)
    │   ├── HashSet<String> exact domain matching (O(1))
    │   ├── Suffix matching (blocks subdomains automatically)
    │   ├── Whitelist support (@@rules)
    │   └── User custom rules
    │
    └── Filter List Manager (FilterListManager)
        ├── Parses: Hosts files, Domain lists, EasyList/ABP format
        ├── Auto-updates via WorkManager (every 12h, WiFi only)
        └── Persists to Room database
```

## Technical Details

- **Blocking method**: DNS VPN — only DNS traffic routes through VPN; browsing/streaming traffic bypasses it entirely for minimal battery impact
- **DNS protocol**: DNS-over-HTTPS (DoH) using OkHttp — encrypted, private
- **Filter matching**: O(1) HashSet for exact domains + suffix walk for subdomains
- **Battery impact**: Minimal — only DNS packets (tiny) pass through the VPN interface
- **Filter capacity**: Tested up to 1M+ rules with sub-millisecond lookup

## Building

Requirements: Android Studio Ladybug+, JDK 17

```bash
git clone https://github.com/the-no-hands-company/adblocker
cd adblocker
./gradlew assembleDebug
```

## Status: TNHC Side Project

Shield is a TNHC side project — same relationship as DevTrack to the main NSCP platform. It is maintained independently and is not part of the Nexus Systems Cloud Platform.

**Maintainer**: The No Hands Company  
**License**: MIT  
**Min Android**: 8.0 (API 26)
