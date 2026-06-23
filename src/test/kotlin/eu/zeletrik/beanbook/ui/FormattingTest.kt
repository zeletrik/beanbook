package eu.zeletrik.beanbook.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Tests for the URL formatting helpers [toHref] and [toDisplayLink]. */
class FormattingTest {

    @Test
    fun `toHref returns null for blank input`() {
        assertNull("".toHref())
        assertNull("   ".toHref())
        assertNull(null.toHref())
    }

    @Test
    fun `toHref keeps existing http and https schemes`() {
        assertEquals("https://roaster.com/beans", "https://roaster.com/beans".toHref())
        assertEquals("http://roaster.com", "http://roaster.com".toHref())
    }

    @Test
    fun `toHref prepends https when scheme is missing`() {
        assertEquals("https://roaster.com/beans", "roaster.com/beans".toHref())
        assertEquals("https://roaster.com", "  roaster.com  ".toHref())
    }

    @Test
    fun `toHref rejects unsafe schemes`() {
        assertNull("javascript:alert(1)".toHref())
        assertNull("mailto:hi@example.com".toHref())
        assertNull("ftp://example.com".toHref())
    }

    @Test
    fun `toDisplayLink strips scheme and www and trailing slash`() {
        assertEquals("roaster.com/beans", "https://www.roaster.com/beans".toDisplayLink())
        assertEquals("roaster.com", "http://roaster.com/".toDisplayLink())
    }
}
