package eu.zeletrik.beanbook.ai.internal

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Confirms the fetcher delegates URL safety to [UrlSafetyGuard] before sending anything (the guard's own
 * rules are covered exhaustively in [UrlSafetyGuardTest]). The success path needs a live server, so the
 * service is tested against a stub [PageFetcher] instead.
 */
class ProductPageFetcherTest {

    private val fetcher = ProductPageFetcher()

    @Test
    fun `rejects disallowed schemes and embedded credentials`() {
        assertNull(fetcher.fetch("ftp://example.com/beans"))
        assertNull(fetcher.fetch("https://user:pass@example.com/beans"))
        assertNull(fetcher.fetch("   "))
    }

    @Test
    fun `rejects internal SSRF targets before any request is sent`() {
        assertNull(fetcher.fetch("http://127.0.0.1/x"))
        assertNull(fetcher.fetch("http://169.254.169.254/latest/meta-data/"))
        assertNull(fetcher.fetch("http://10.0.0.5/x"))
    }
}
