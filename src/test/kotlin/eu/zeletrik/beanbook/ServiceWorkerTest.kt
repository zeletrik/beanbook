package eu.zeletrik.beanbook

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import org.junit.jupiter.api.Assertions.assertEquals

/** Verifies that the PWA service worker, registration script, and offline fallback page are served correctly over HTTP. */
// Pin auth off: these are public PWA assets and the test must not depend on the security default.
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = ["beanbook.security.enabled=false"],
)
class ServiceWorkerTest {

    @LocalServerPort
    private var port: Int = 0

    private val restTemplate = RestTemplate()

    private fun get(path: String) =
        restTemplate.getForEntity("http://localhost:$port$path", String::class.java)

    // AC-19: sw.js is served (existence check)
    @Test
    fun `sw_js is served with HTTP 200`() {
        assertEquals(HttpStatus.OK, get("/sw.js").statusCode)
    }

    // AC-19: sw.js body contains the service worker implementation (install + fetch handlers)
    @Test
    fun `sw_js contains install and fetch event handlers`() {
        val body = requireNotNull(get("/sw.js").body)
        assertTrue(body.contains("install"), "sw.js must contain install handler")
        assertTrue(body.contains("fetch"), "sw.js must contain fetch handler")
        assertTrue(body.contains("caches"), "sw.js must use Cache API")
    }

    // AC-19: registration call is injected into the served page HTML
    @Test
    fun `page HTML contains service worker registration script`() {
        val body = requireNotNull(get("/").body)
        assertTrue(
            body.contains("navigator.serviceWorker.register"),
            "Page HTML must contain navigator.serviceWorker.register"
        )
    }

    // AC-22: offline.html is served
    @Test
    fun `offline_html is served with HTTP 200`() {
        assertEquals(HttpStatus.OK, get("/offline.html").statusCode)
    }

    // AC-22: offline.html contains the app name
    @Test
    fun `offline_html contains Bean Book`() {
        val body = requireNotNull(get("/offline.html").body)
        assertTrue(body.contains("Bean Book"), "offline.html must contain 'Bean Book'")
    }

    // AC-22: offline.html communicates the offline status
    @Test
    fun `offline_html contains offline message`() {
        val body = requireNotNull(get("/offline.html").body)
        assertTrue(body.contains("offline"), "offline.html must contain the word 'offline'")
    }

    // AC-23: offline.html does not reference Vaadin bundle paths
    @Test
    fun `offline_html does not reference VAADIN paths`() {
        val body = requireNotNull(get("/offline.html").body)
        assertFalse(body.contains("/VAADIN/"), "offline.html must not reference /VAADIN/ paths")
    }

    // AC-20 and AC-21: browser-level service worker behavior — manual verification required.
    // Service worker install caching and offline interception cannot be automated without a live browser.
    // Verified manually on iOS Safari: add app to home screen, disable network, reopen → offline.html appears.
}
