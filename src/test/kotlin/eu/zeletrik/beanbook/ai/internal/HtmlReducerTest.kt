package eu.zeletrik.beanbook.ai.internal

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Verifies [reduceHtml] strips chrome/markup and keeps the visible text. */
class HtmlReducerTest {

    @Test
    fun `drops scripts and styles but keeps the visible body text`() {
        val html = """
            <html><head>
              <style>.x{color:red}</style>
              <script>var secret = 42;</script>
            </head><body>
              <h1>Ethiopia Yirgacheffe</h1>
              <p>Washed &amp; floral, 250g</p>
            </body></html>
        """.trimIndent()

        val text = reduceHtml(html)

        assertTrue(text.contains("Ethiopia Yirgacheffe"), "Heading text kept")
        assertTrue(text.contains("Washed & floral, 250g"), "Body text kept and the entity unescaped")
        assertFalse(text.contains("secret"), "Script contents dropped")
        assertFalse(text.contains("color:red"), "Style contents dropped")
        assertFalse(text.contains("<"), "All tags stripped")
    }
}
