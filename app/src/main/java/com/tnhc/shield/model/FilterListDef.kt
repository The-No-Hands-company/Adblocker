package com.tnhc.shield.model

import com.tnhc.shield.filter.FilterFormat
import kotlinx.serialization.Serializable

@Serializable
data class FilterListDef(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val format: FilterFormat = FilterFormat.AUTO,
    val category: FilterCategory = FilterCategory.ADS,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false
)

enum class FilterCategory(val displayName: String) {
    ADS("Ads"),
    TRACKING("Tracking"),
    MALWARE("Malware"),
    SOCIAL("Social"),
    ANNOYANCES("Annoyances"),
    PRIVACY("Privacy"),
    MOBILE_GAMING("Mobile & Gaming"),
    CUSTOM("Custom")
}

object DefaultFilterLists {
    val builtIn = listOf(
        FilterListDef(
            id = "oisd_full",
            name = "OISD Full",
            description = "One of the most comprehensive blocklists. Covers ads, tracking, malware.",
            url = "https://big.oisd.nl/",
            format = FilterFormat.DOMAIN_LIST,
            category = FilterCategory.ADS,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "adguard_base",
            name = "AdGuard Base",
            description = "AdGuard's main filter — optimized for Android.",
            url = "https://filters.adtidy.org/android/filters/2_optimized.txt",
            format = FilterFormat.ADBLOCK,
            category = FilterCategory.ADS,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "adguard_mobile",
            name = "AdGuard Mobile Ads",
            description = "Mobile-specific ad networks and in-app ads.",
            url = "https://filters.adtidy.org/android/filters/11_optimized.txt",
            format = FilterFormat.ADBLOCK,
            category = FilterCategory.MOBILE_GAMING,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "goodbye_ads_mobile",
            name = "Goodbye Ads (Mobile)",
            description = "Aggressive mobile-focused list. Covers Unity, AppLovin, ironSource and 400+ in-app ad SDKs.",
            url = "https://raw.githubusercontent.com/jerryn70/GoodbyeAds/master/Hosts/GoodbyeAds.txt",
            format = FilterFormat.HOSTS,
            category = FilterCategory.MOBILE_GAMING,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "hagezi_pro_plus",
            name = "HaGeZi Pro Plus",
            description = "Extended professional blocklist with strong mobile & gaming ad coverage.",
            url = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/pro.plus.txt",
            format = FilterFormat.DOMAIN_LIST,
            category = FilterCategory.MOBILE_GAMING,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "easylist",
            name = "EasyList",
            description = "The most popular ad blocking list. Removes ads from websites.",
            url = "https://easylist.to/easylist/easylist.txt",
            format = FilterFormat.ADBLOCK,
            category = FilterCategory.ADS,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "easyprivacy",
            name = "EasyPrivacy",
            description = "Removes all forms of tracking from the internet.",
            url = "https://easylist.to/easylist/easyprivacy.txt",
            format = FilterFormat.ADBLOCK,
            category = FilterCategory.TRACKING,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "steven_black",
            name = "Steven Black Hosts",
            description = "Hosts-based combined list: adware + malware.",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            format = FilterFormat.HOSTS,
            category = FilterCategory.ADS,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "adguard_tracking",
            name = "AdGuard Tracking Protection",
            description = "Extensive list of analytics, tracking scripts and pixels.",
            url = "https://filters.adtidy.org/android/filters/3_optimized.txt",
            format = FilterFormat.ADBLOCK,
            category = FilterCategory.TRACKING,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "malware_domains",
            name = "Malware Domains",
            description = "Blocks known malware distribution sites.",
            url = "https://filters.adtidy.org/android/filters/9_optimized.txt",
            format = FilterFormat.ADBLOCK,
            category = FilterCategory.MALWARE,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "hagezi_pro",
            name = "HaGeZi Pro",
            description = "Professional-grade blocklist with high coverage.",
            url = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/pro.txt",
            format = FilterFormat.DOMAIN_LIST,
            category = FilterCategory.PRIVACY,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "fanboy_annoyances",
            name = "Fanboy Annoyances",
            description = "Blocks cookie notices, newsletter pop-ups, and other annoyances.",
            url = "https://easylist.to/easylist/fanboy-annoyance.txt",
            format = FilterFormat.ADBLOCK,
            category = FilterCategory.ANNOYANCES,
            isBuiltIn = true
        ),
        FilterListDef(
            id = "adguard_social",
            name = "AdGuard Social Media",
            description = "Removes social share buttons and tracking widgets.",
            url = "https://filters.adtidy.org/android/filters/4_optimized.txt",
            format = FilterFormat.ADBLOCK,
            category = FilterCategory.SOCIAL,
            enabled = false,
            isBuiltIn = true
        )
    )
}
