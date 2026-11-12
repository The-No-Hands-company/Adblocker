package com.tnhc.shield.filter

object FilterListParser {

    data class ParseResult(
        val blockedDomains: Set<String>,
        val whitelistedDomains: Set<String>
    )

    fun parse(content: String, format: FilterFormat): ParseResult {
        val blocked = mutableSetOf<String>()
        val whitelisted = mutableSetOf<String>()

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!") || line.startsWith("[")) return@forEach

            when (format) {
                FilterFormat.HOSTS -> parseHostsLine(line, blocked)
                FilterFormat.DOMAIN_LIST -> parseDomainListLine(line, blocked)
                FilterFormat.ADBLOCK -> parseAdblockLine(line, blocked, whitelisted)
                FilterFormat.AUTO -> {
                    when {
                        line.startsWith("||") || line.startsWith("@@") -> parseAdblockLine(line, blocked, whitelisted)
                        line.first().isDigit() -> parseHostsLine(line, blocked)
                        else -> parseDomainListLine(line, blocked)
                    }
                }
            }
        }

        return ParseResult(blocked, whitelisted)
    }

    private fun parseHostsLine(line: String, blocked: MutableSet<String>) {
        // "0.0.0.0 ads.example.com" or "127.0.0.1 ads.example.com"
        val parts = line.split(Regex("\\s+"))
        if (parts.size < 2) return
        val ip = parts[0]
        if (ip != "0.0.0.0" && ip != "127.0.0.1" && ip != "::" && ip != "::1") return
        val domain = parts[1].lowercase().trimEnd('.')
        if (isValidDomain(domain) && domain != "localhost") {
            blocked.add(domain)
        }
    }

    private fun parseDomainListLine(line: String, blocked: MutableSet<String>) {
        val domain = line.lowercase().trimEnd('.').removePrefix("*.")
        if (isValidDomain(domain)) blocked.add(domain)
    }

    private fun parseAdblockLine(
        line: String,
        blocked: MutableSet<String>,
        whitelisted: MutableSet<String>
    ) {
        val isWhitelist = line.startsWith("@@")
        val rule = if (isWhitelist) line.removePrefix("@@") else line

        // Only process domain-anchor rules: ||example.com^
        if (!rule.startsWith("||")) return

        val withoutAnchor = rule.removePrefix("||")
        val domainPart = withoutAnchor
            .removePrefix("*.")
            .substringBefore("^")
            .substringBefore("/")
            .substringBefore("$")
            .lowercase()
            .trim()

        if (!isValidDomain(domainPart)) return

        if (isWhitelist) whitelisted.add(domainPart)
        else blocked.add(domainPart)
    }

    private fun isValidDomain(domain: String): Boolean {
        if (domain.isEmpty() || domain.length > 253) return false
        if (domain.startsWith("-") || domain.endsWith("-")) return false
        return domain.all { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
    }
}

enum class FilterFormat {
    HOSTS,
    DOMAIN_LIST,
    ADBLOCK,
    AUTO
}
