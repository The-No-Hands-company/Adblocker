# Shield — TNHC Adblocker

> **Your Digital Immune System** — the best free adblocker for Android, by The No Hands Company.

## What it does

Shield blocks ads, trackers, malware, and annoyances system-wide on Android — across **every app**, not just your browser — using a local VPN DNS filter. No root required. No cloud dependency. Zero telemetry.

## Why it's better than paid alternatives

| Feature | Shield (free) | AdGuard (€2.99/mo) | Blokada 6 (freemium) | NextDNS ($2/mo) |
|---|---|---|---|---|
| System-wide blocking | ✅ | ✅ | ✅ | ✅ |
| DNS-over-HTTPS | ✅ | ✅ | ✅ | ✅ |
| 12+ curated filter lists | ✅ | 4 default | 1 (free tier) | Config required |
| No cloud dependency | ✅ | ❌ | ❌ | ❌ |
| No subscription | ✅ | ❌ | ❌ | ❌ |
| No telemetry | ✅ | Partial | ❌ | ❌ |
| Auto-updating lists | ✅ | ✅ | ✅ | ✅ |
| Material You UI | ✅ | ❌ | ❌ | N/A |
| Open source | ✅ | Partial | ❌ | ❌ |

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
