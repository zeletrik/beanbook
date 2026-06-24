package eu.zeletrik.beanbook.ai.internal

import org.slf4j.LoggerFactory
import java.io.Reader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Outcome of a single HTTP request inside the redirect loop. */
private sealed interface Hop {
    data class Body(val html: String) : Hop
    data class Redirect(val to: URI) : Hop
    data object Fail : Hop
}

/**
 * Fetches product/roaster page HTML with the JDK HTTP client (no extra dependency). Conservative by
 * design: a descriptive User-Agent, a short timeout, and the body truncated so an oversized page can't
 * exhaust memory. Any failure (bad URL, timeout, non-2xx) → null.
 *
 * URL safety (scheme, credentials, and SSRF address checks) is delegated to [UrlSafetyGuard]. Redirects
 * are followed manually — the JDK client is set to never auto-follow — so every hop is re-validated
 * through the guard before it is requested.
 */
internal class ProductPageFetcher(
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build(),
    private val guard: UrlSafetyGuard = UrlSafetyGuard(),
) : PageFetcher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun fetch(url: String): String? {
        var uri = guard.parse(url)
        var hops = 0
        while (uri != null && hops <= MAX_REDIRECTS) {
            if (!guard.isPubliclyRoutable(uri)) {
                log.warn("Refusing to fetch {} — host resolves to a private/loopback address (SSRF guard)", uri)
                return null
            }
            when (val hop = requestOnce(uri)) {
                is Hop.Body -> return hop.html
                is Hop.Redirect -> { uri = guard.parse(hop.to.toString()); hops++ }
                Hop.Fail -> return null
            }
        }
        if (uri != null) log.warn("Refusing to fetch {} — too many redirects", url)
        return null
    }

    private fun requestOnce(uri: URI): Hop {
        val request = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .GET()
            .build()
        val response = try {
            client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        } catch (e: Exception) {
            log.warn("Page fetch for {} failed", uri, e)
            return Hop.Fail
        }
        val status = response.statusCode()
        return when {
            status in HTTP_OK_RANGE ->
                Hop.Body(response.body().use { it.reader(Charsets.UTF_8).readUpTo(MAX_BODY_CHARS) })
            status in HTTP_REDIRECT_CODES -> {
                response.body().close()
                val location = response.headers().firstValue("Location").orElse(null)
                if (location.isNullOrBlank()) Hop.Fail else Hop.Redirect(uri.resolve(location))
            }
            else -> {
                response.body().close()
                log.warn("Page fetch for {} returned status {}", uri, status)
                Hop.Fail
            }
        }
    }

    private fun Reader.readUpTo(maxChars: Int): String {
        val out = StringBuilder()
        val chunk = CharArray(CHUNK_CHARS)
        while (out.length < maxChars) {
            val read = read(chunk, 0, minOf(chunk.size, maxChars - out.length))
            if (read < 0) break
            out.append(chunk, 0, read)
        }
        return out.toString()
    }

    companion object {
        private val REQUEST_TIMEOUT = Duration.ofSeconds(10)
        private const val MAX_BODY_CHARS = 2_000_000
        private const val CHUNK_CHARS = 8_192
        private const val MAX_REDIRECTS = 3
        private val HTTP_OK_RANGE = 200..299
        private val HTTP_REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private const val USER_AGENT = "BeanBook/1.0 (self-hosted coffee bean entry assistant)"
    }
}
