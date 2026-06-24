package eu.zeletrik.beanbook.ai.internal

import java.net.InetAddress
import java.net.URI

/**
 * Validates outbound URLs before they are fetched, to prevent SSRF. A URL is allowed only when it is
 * http/https, has a real host, carries no embedded credentials, and resolves exclusively to publicly
 * routable addresses — never loopback, link-local (incl. the cloud metadata IP), private/site-local,
 * CGNAT, or IPv6 unique-local. Kept separate from [ProductPageFetcher] so it can be tested directly and
 * reused by any future feature that fetches user-supplied URLs (e.g. the wishlist web scanner).
 */
internal class UrlSafetyGuard {

    /**
     * Parses [raw] into a safe http/https [URI] (a bare host defaults to https), or `null` if the scheme
     * isn't http/https, the host is missing, or it embeds credentials (`user:pass@host`) — which would
     * also leak into logs. Does not check the host's address; pair with [isPubliclyRoutable].
     */
    fun parse(raw: String): URI? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return runCatching { URI.create(withScheme) }.getOrNull()
            ?.takeIf { it.scheme?.lowercase() in ALLOWED_SCHEMES && !it.host.isNullOrBlank() && it.userInfo == null }
    }

    /**
     * True when [uri]'s host resolves and every resolved address is publicly routable. An unresolvable
     * host, or one that resolves to any internal address, returns `false` (i.e. must not be fetched).
     */
    fun isPubliclyRoutable(uri: URI): Boolean {
        val host = uri.host ?: return false
        val addresses = runCatching { InetAddress.getAllByName(host.removeSurrounding("[", "]")) }.getOrNull()
            ?: return false
        return addresses.none { it.isNonPublic() }
    }

    private fun InetAddress.isNonPublic(): Boolean =
        isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress ||
            isAnyLocalAddress || isMulticastAddress || isCgnatOrUniqueLocal()

    /** Covers ranges `InetAddress` flags miss: 100.64.0.0/10 (CGNAT) and fc00::/7 (IPv6 unique-local). */
    private fun InetAddress.isCgnatOrUniqueLocal(): Boolean {
        val bytes = address
        return when (bytes.size) {
            IPV4_LEN ->
                (bytes[0].toInt() and BYTE_MASK) == CGNAT_FIRST_OCTET &&
                    (bytes[1].toInt() and BYTE_MASK) in CGNAT_SECOND_RANGE
            IPV6_LEN -> (bytes[0].toInt() and ULA_MASK) == ULA_PREFIX
            else -> false
        }
    }

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")
        private const val IPV4_LEN = 4
        private const val IPV6_LEN = 16
        private const val CGNAT_FIRST_OCTET = 100
        private val CGNAT_SECOND_RANGE = 64..127
        private const val BYTE_MASK = 0xFF
        private const val ULA_MASK = 0xFE
        private const val ULA_PREFIX = 0xFC
    }
}
