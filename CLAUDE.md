# Shield — TNHC Adblocker

## Project

TNHC side project. Android adblocker using VPN DNS filtering. Package: `com.tnhc.shield`.  
Not part of NSCP. Similar positioning to DevTrack within the TNHC ecosystem.

## Build

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK (needs signing config)
./gradlew lint              # lint checks
```

**Requires**: Android Studio Ladybug+, JDK 17, Android SDK with API 35

## Key Architecture

```
app/src/main/java/com/tnhc/shield/
├── vpn/ShieldVpnService.kt     # VPN service — intercepts DNS via TUN, IPv4/IPv6 dual-stack, channel sync
├── service/ShieldTileService.kt # Android Quick Settings Tile for 1-tap toggling
├── dns/DnsEngine.kt            # DNS query handler (block/forward/cache, live query logging)
├── dns/DnsPacket.kt            # IP/UDP/DNS packet parsing and building (IPv4 & IPv6 UDP checksums)
├── dns/DohClient.kt            # DNS-over-HTTPS client with dynamic server switching
├── dns/BootstrapDns.kt         # Static bootstrap IP mapping for DoH endpoints
├── dns/SniParser.kt            # TLS ClientHello SNI extractor for port 443 packets
├── filter/FilterEngine.kt      # HashSet-based domain blocklist (O(1) lookup + custom rules)
├── filter/IpBlocklistEngine.kt # CIDR IP subnet blocklist for hardcoded ad SDK edge servers
├── filter/FilterListParser.kt  # Parses hosts/domain-list/EasyList formats
├── filter/FilterListManager.kt # Downloads filter lists + cold-start starter blocklist
├── data/db/                    # Room database (block events, filter lists, custom rules, DNS logs)
├── data/prefs/ShieldPreferences.kt  # DataStore preferences
├── stats/StatsRepository.kt    # Block event tracking
├── work/FilterUpdateWorker.kt  # Background filter list updates (WorkManager)
├── ui/MainViewModel.kt         # Main ViewModel (custom rules, query logs, DoH dynamic switching)
└── ui/screens/                 # Compose screens: home, dashboard (live query log), filters (custom rules), apps, settings
```

## How the VPN Works

1. `ShieldVpnService` creates a TUN interface at `10.0.0.2/32`
2. `addDnsServer("10.0.0.2")` forces all app DNS through the TUN
3. `addRoute("10.0.0.2", 32)` routes only DNS traffic — internet traffic is direct
4. VPN reads UDP packets destined for `10.0.0.2:53`
5. `DnsEngine.handleDnsQuery()` checks domain against `FilterEngine`
6. Blocked → NXDOMAIN response written back to TUN
7. Allowed → forwarded via `DohClient` (protected socket bypasses VPN)

## Default Filter Lists

13 lists in `model/FilterListDef.kt > DefaultFilterLists.builtIn`. Downloaded on first run via `FilterUpdateWorker`. Key lists: OISD Full, EasyList, EasyPrivacy, AdGuard Base/Mobile/Tracking, Steven Black, HaGeZi Pro, HaGeZi Pro Plus, Goodbye Ads (Mobile).

Filter categories: `ADS`, `TRACKING`, `MALWARE`, `SOCIAL`, `ANNOYANCES`, `PRIVACY`, `MOBILE_GAMING`, `CUSTOM`.

`FilterEngine.builtInBlockedExact` — hardcoded set of ~45 game/in-app ad SDK domains (Unity Ads, AppLovin/MAX, IronSource/LevelPlay, Vungle, Chartboost, AdColony, Tapjoy, InMobi, Pangle, Moloco, Fyber, Smaato). These survive filter list reloads and are always blocked.

**DNS blocking limitation**: ~70% of in-app ads blocked via DNS. Some SDK builds (especially Unity Ads) embed hardcoded IP addresses that bypass DNS entirely. Full coverage requires v1.2 SNI inspection or v2.0 MITM proxy.

## Dev Notes

- `FilterEngine` is thread-safe via `ReentrantReadWriteLock`
- DNS cache max 2048 entries, TTL-aware eviction
- `BootReceiver` handles auto-start if enabled in settings
- All screens share `MainViewModel` via `hiltViewModel()`
- Material You dynamic color via `dynamicColorScheme()` on Android 12+
- `minSdk = 26` (Android 8.0 Oreo)

## Roadmap

- v1.1: ✅ Per-app split tunneling — `addDisallowedApplication()` via Apps screen
- v1.2: ✅ HTTPS SNI inspection (`SniParser.kt` — parse TLS ClientHello SNI hostnames)
- v1.3: ✅ Custom rule editor UI + Real-time Live Query Log + Android Quick Settings Tile
- v1.4: ✅ In-app & in-game ad blocking improvements
  - Hardcoded SDK domains in `FilterEngine.builtInBlockedExact`
  - Starter compact blocklist in `assets/starter_blocklist.txt` for zero-day cold-start protection
  - CIDR IP-level blocking for ad SDK edge clusters (`IpBlocklistEngine.kt`)
  - Dual-stack IPv6 interface & DNS route (`fd00::1`) to eliminate IPv6 DNS leaks
  - Dynamic DoH server switching with static bootstrap IP resolver (`BootstrapDns.kt`)
  - Channel-based synchronized TUN writer to prevent file descriptor corruption
- v1.5: ✅ Deep App Attribution & Visibility
  - Per-app DNS Attribution via `ConnectivityManager.getConnectionOwnerUid()` with package/app name chips in Live Query Log
  - 1-tap "Exclude App" split-tunneling toggle directly from query log rows
  - Custom DoH endpoint input in Settings (support for NextDNS, ControlD, Pi-hole, AdGuard Home)
  - Manual "Update All Lists" action in Filters screen with real-time progress bar
- v1.6: ✅ Cosmetic Collapse & Configuration Portability
  - Dummy Ad Responder (`DummyAdResponder.kt` with 1x1 transparent GIF89a & empty JSON)
  - JSON Backup & Restore for Custom Rules, Excluded Apps, and Settings
- v2.0: ✅ Release Engineering, In-App Auto-Updater & F-Droid Readiness
  - In-App GitHub Release Update Checker (`UpdateChecker.kt`)
  - Automated GitHub Actions APK release pipeline on tag push
  - F-Droid Fastlane metadata structure (`fastlane/metadata/android/en-US/`)
- Future: Optional HTTPS MITM proxy for full content cosmetic DOM filtering
