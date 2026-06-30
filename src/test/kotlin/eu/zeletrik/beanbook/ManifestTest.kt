package eu.zeletrik.beanbook

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate

/** Verifies that the PWA manifest and app icons are served with the expected status, content types, and metadata. */
// Pin auth off: the manifest and icons are public PWA assets; this test must not depend on the
// deployment's security default (which can be flipped on via beanbook.security / env vars).
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = ["beanbook.security.enabled=false"],
)
class ManifestTest {

    @LocalServerPort
    private var port: Int = 0

    private val restTemplate = RestTemplate()

    private fun get(path: String) =
        restTemplate.getForEntity("http://localhost:$port$path", String::class.java)

    // AC-16: manifest served — HTTP 200
    @Test
    fun `manifest webmanifest is served with HTTP 200`() {
        assertEquals(HttpStatus.OK, get("/manifest.webmanifest").statusCode)
    }

    // AC-16: correct Content-Type so browsers accept the manifest
    @Test
    fun `manifest is served with application manifest+json content type`() {
        val contentType = get("/manifest.webmanifest").headers.contentType?.toString() ?: ""
        assertTrue(
            contentType.contains("application/manifest+json"),
            "Expected Content-Type 'application/manifest+json' but was '$contentType'"
        )
    }

    // AC-18: correct app names
    @Test
    fun `manifest declares name as Bean Book and short_name as BeanBook`() {
        val body = requireNotNull(get("/manifest.webmanifest").body)
        assertTrue(body.contains("Bean Book"), "manifest name must be 'Bean Book'")
        assertTrue(body.contains("BeanBook"), "manifest short_name must be 'BeanBook'")
    }

    // AC-19: non-empty theme color (original test kept, updated assertion)
    @Test
    fun `manifest declares a non-empty theme color`() {
        val body = requireNotNull(get("/manifest.webmanifest").body)
        val match = Regex("\"theme_color\"\\s*:\\s*\"([^\"]+)\"").find(body)
        assertNotNull(match, "manifest must declare a theme_color field with a string value")
        assertTrue(match!!.groupValues[1].isNotBlank(), "theme_color value must not be blank")
    }

    // ── New icon and manifest assertions (AC-12 through AC-21) ──

    // AC-12: 192×192 SVG icon served with HTTP 200
    @Test
    fun `icon-192 svg is served with HTTP 200`() {
        assertEquals(HttpStatus.OK, get("/icons/icon-192.svg").statusCode)
    }

    // AC-12: 192×192 SVG icon served with correct Content-Type
    @Test
    fun `icon-192 svg is served with image svg+xml content type`() {
        val contentType = get("/icons/icon-192.svg").headers.contentType?.toString() ?: ""
        assertTrue(contentType.contains("image/svg+xml"),
            "Expected image/svg+xml but was '$contentType'")
    }

    // AC-13: 512×512 SVG icon served with HTTP 200
    @Test
    fun `icon-512 svg is served with HTTP 200`() {
        assertEquals(HttpStatus.OK, get("/icons/icon-512.svg").statusCode)
    }

    // AC-13: 512×512 SVG icon served with correct Content-Type
    @Test
    fun `icon-512 svg is served with image svg+xml content type`() {
        val contentType = get("/icons/icon-512.svg").headers.contentType?.toString() ?: ""
        assertTrue(contentType.contains("image/svg+xml"),
            "Expected image/svg+xml but was '$contentType'")
    }

    // AC-14: icon sits on the espresso brand background (issue #4 brand refresh).
    @Test
    fun `icon-192 svg uses the espresso brand background`() {
        val body = requireNotNull(get("/icons/icon-192.svg").body)
        assertTrue(
            body.contains("#1F110A") || body.contains("#0E0704"),
            "icon-192 must use the espresso brand background (#1F110A / #0E0704)",
        )
    }

    // AC-15: icon is the vector bean/book mark with the warm-roast accent, not the old emoji placeholder.
    @Test
    fun `icon-192 svg is a vector mark with the warm roast accent`() {
        val body = requireNotNull(get("/icons/icon-192.svg").body)
        assertTrue(body.contains("<path"), "icon-192 must be a vector mark (paths), not text/emoji")
        assertTrue(
            body.contains("#E76F51") || body.contains("#F4A261"),
            "icon-192 must include the warm roast accent (#E76F51 / #F4A261)",
        )
    }

    // AC-15: an SVG favicon is served for the browser tab.
    @Test
    fun `favicon svg is served as image svg+xml`() {
        val resp = get("/icons/favicon.svg")
        assertEquals(HttpStatus.OK, resp.statusCode)
        val contentType = resp.headers.contentType?.toString() ?: ""
        assertTrue(contentType.contains("image/svg+xml"), "Expected image/svg+xml but was '$contentType'")
    }

    // AC-16: manifest icons array contains 192×192 SVG entry
    @Test
    fun `manifest declares 192x192 svg icon entry`() {
        val body = requireNotNull(get("/manifest.webmanifest").body)
        assertTrue(body.contains("/icons/icon-192.svg"), "manifest must reference icon-192.svg")
        assertTrue(body.contains("192x192"), "manifest must declare 192x192 size")
        assertTrue(body.contains("image/svg+xml"), "manifest icon must have svg type")
    }

    // AC-17: manifest icons array contains 512×512 SVG entry
    @Test
    fun `manifest declares 512x512 svg icon entry`() {
        val body = requireNotNull(get("/manifest.webmanifest").body)
        assertTrue(body.contains("/icons/icon-512.svg"), "manifest must reference icon-512.svg")
        assertTrue(body.contains("512x512"), "manifest must declare 512x512 size")
    }

    // AC-18: manifest shortcut name and URL
    @Test
    fun `manifest declares Add Purchase shortcut`() {
        val body = requireNotNull(get("/manifest.webmanifest").body)
        assertTrue(body.contains("Add Purchase"), "manifest must contain shortcut name 'Add Purchase'")
        assertTrue(body.contains("\"shortcuts\""), "manifest must contain shortcuts array")
    }

    // AC-19: manifest shortcut references app icon
    @Test
    fun `manifest shortcut references app icon`() {
        val body = requireNotNull(get("/manifest.webmanifest").body)
        val shortcutsIndex = body.indexOf("\"shortcuts\"")
        assertTrue(shortcutsIndex >= 0, "manifest must have shortcuts")
        val shortcutsSection = body.substring(shortcutsIndex)
        assertTrue(shortcutsSection.contains("icon-192.svg"), "shortcut must reference icon-192.svg")
    }

    // AC-20: theme_color is the espresso brand colour
    @Test
    fun `manifest theme_color is espresso`() {
        val body = requireNotNull(get("/manifest.webmanifest").body)
        assertTrue(body.contains("\"theme_color\": \"#120a07\"") || body.contains("\"theme_color\":\"#120a07\""),
            "theme_color must be #120a07")
    }

    // AC-21: background_color (splash) is the espresso void so the dark icon blends in
    @Test
    fun `manifest background_color is espresso void`() {
        val body = requireNotNull(get("/manifest.webmanifest").body)
        assertTrue(body.contains("\"background_color\": \"#0A0503\"") || body.contains("\"background_color\":\"#0A0503\""),
            "background_color must be #0A0503")
    }
}
