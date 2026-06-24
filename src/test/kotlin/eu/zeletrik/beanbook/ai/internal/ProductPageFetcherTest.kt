package eu.zeletrik.beanbook.ai.internal

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Covers the URL validation that runs before any network call. The success path needs a live server, so
 * it isn't unit-tested here (the service is tested against a stub [PageFetcher] instead).
 */
class ProductPageFetcherTest {

    private val fetcher = ProductPageFetcher()

    @Test
    fun `rejects URLs with embedded credentials (no request, no leak into logs)`() {
        assertNull(fetcher.fetch("https://user:pass@example.com/beans"))
    }

    @Test
    fun `rejects non-http schemes`() {
        assertNull(fetcher.fetch("ftp://example.com/beans"))
        assertNull(fetcher.fetch("file:///etc/passwd"))
    }

    @Test
    fun `rejects blank input`() {
        assertNull(fetcher.fetch("   "))
    }
}
