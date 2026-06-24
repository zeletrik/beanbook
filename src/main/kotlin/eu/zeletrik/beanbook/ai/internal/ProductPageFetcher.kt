package eu.zeletrik.beanbook.ai.internal

import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Fetches product/roaster page HTML with the JDK HTTP client (no extra dependency). Conservative by
 * design: http/https only, a descriptive User-Agent, a short timeout, redirects followed, and the body
 * truncated so an oversized page can't exhaust memory. Any failure (bad URL, timeout, non-2xx) → null.
 */
class ProductPageFetcher(
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build(),
) : PageFetcher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun fetch(url: String): String? {
        val uri = toHttpUri(url) ?: return null
        val request = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .GET()
            .build()
        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in HTTP_OK_RANGE) {
                log.warn("Page fetch for {} returned status {}", uri, response.statusCode())
                return null
            }
            response.body().take(MAX_BODY_CHARS)
        } catch (e: Exception) {
            log.warn("Page fetch for {} failed", uri, e)
            null
        }
    }

    /** Accepts a bare host (defaults to https) or an explicit http/https URL; rejects every other scheme. */
    private fun toHttpUri(raw: String): URI? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return runCatching { URI.create(withScheme) }.getOrNull()
            ?.takeIf { it.scheme?.lowercase() in ALLOWED_SCHEMES && !it.host.isNullOrBlank() }
    }

    companion object {
        private val REQUEST_TIMEOUT = Duration.ofSeconds(10)
        private const val MAX_BODY_CHARS = 2_000_000
        private val HTTP_OK_RANGE = 200..299
        private val ALLOWED_SCHEMES = setOf("http", "https")
        private const val USER_AGENT = "BeanBook/1.0 (self-hosted coffee bean entry assistant)"
    }
}
