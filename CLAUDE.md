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
├── vpn/ShieldVpnService.kt     # VPN service — intercepts DNS via TUN interface
├── dns/DnsEngine.kt            # DNS query handler (block/forward/cache)
├── dns/DnsPacket.kt            # IP/UDP/DNS packet parsing and building
├── dns/DohClient.kt            # DNS-over-HTTPS client
├── filter/FilterEngine.kt      # HashSet-based domain blocklist (O(1) lookup)
├── filter/FilterListParser.kt  # Parses hosts/domain-list/EasyList formats
├── filter/FilterListManager.kt # Downloads and manages filter lists
├── data/db/                    # Room database (block events, filter lists)
├── data/prefs/ShieldPreferences.kt  # DataStore preferences
├── stats/StatsRepository.kt    # Block event tracking
├── work/FilterUpdateWorker.kt  # Background filter list updates (WorkManager)
├── ui/MainViewModel.kt         # Main ViewModel (shared across all screens)
└── ui/screens/                 # Compose screens: home, dashboard, filters, apps, settings
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

12 lists included in `model/FilterListDef.kt > DefaultFilterLists.builtIn`. Downloaded on first run via `FilterUpdateWorker`. Key lists: OISD Full, EasyList, EasyPrivacy, AdGuard Base/Mobile/Tracking, Steven Black, HaGeZi Pro.

## Dev Notes

- `FilterEngine` is thread-safe via `ReentrantReadWriteLock`
- DNS cache max 2048 entries, TTL-aware eviction
- `BootReceiver` handles auto-start if enabled in settings
- All screens share `MainViewModel` via `hiltViewModel()`
- Material You dynamic color via `dynamicColorScheme()` on Android 12+
- `minSdk = 26` (Android 8.0 Oreo)

## Roadmap

- v1.1: Per-app split tunneling (disable Shield for specific apps)
- v1.2: HTTPS SNI inspection (block by TLS hostname, not just DNS)
- v1.3: Custom rule editor UI
- v2.0: Optional HTTPS MITM proxy for full content filtering (cosmetic filters)
