package eu.zeletrik.beanbook.ai.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

/** Directly exercises the URL parsing and SSRF address checks (no HTTP client involved). */
class UrlSafetyGuardTest {

    private val guard = UrlSafetyGuard()

    @Test
    fun `a bare host defaults to https`() {
        assertEquals("https", guard.parse("roaster.example/beans")?.scheme)
    }

    @Test
    fun `accepts explicit http and https`() {
        assertNotNull(guard.parse("http://roaster.example/x"))
        assertNotNull(guard.parse("https://roaster.example/x"))
    }

    @Test
    fun `rejects non-http schemes`() {
        assertNull(guard.parse("ftp://roaster.example/x"))
        assertNull(guard.parse("file:///etc/passwd"))
    }

    @Test
    fun `rejects embedded credentials`() {
        assertNull(guard.parse("https://user:pass@roaster.example/x"))
    }

    @Test
    fun `rejects blank and host-less input`() {
        assertNull(guard.parse("   "))
        assertNull(guard.parse("https://"))
    }

    @Test
    fun `allows a public address`() {
        assertTrue(guard.isPubliclyRoutable(URI.create("http://8.8.8.8/x")))
    }

    @Test
    fun `blocks loopback and localhost`() {
        assertFalse(guard.isPubliclyRoutable(URI.create("http://127.0.0.1/x")))
        assertFalse(guard.isPubliclyRoutable(URI.create("http://localhost/x")))
    }

    @Test
    fun `blocks the cloud metadata link-local address`() {
        assertFalse(guard.isPubliclyRoutable(URI.create("http://169.254.169.254/latest/meta-data/")))
    }

    @Test
    fun `blocks private RFC1918 and CGNAT ranges`() {
        assertFalse(guard.isPubliclyRoutable(URI.create("http://10.0.0.5/x")))
        assertFalse(guard.isPubliclyRoutable(URI.create("http://192.168.1.1/x")))
        assertFalse(guard.isPubliclyRoutable(URI.create("http://172.16.0.1/x")))
        assertFalse(guard.isPubliclyRoutable(URI.create("http://100.64.0.1/x")))
    }
}
