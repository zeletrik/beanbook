package eu.zeletrik.beanbook.ai

import ai.koog.prompt.Prompt
import ai.koog.prompt.message.Message
import eu.zeletrik.beanbook.ai.internal.BeanExtractionRunner
import eu.zeletrik.beanbook.ai.internal.PageFetcher
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Exercises [AiExtractionService] against a canned [BeanExtractionRunner] — no real LLM call. */
class AiExtractionServiceTest {

    private val image = byteArrayOf(1, 2, 3)

    @Test
    fun `successful extraction is returned`() {
        val canned = BeanExtraction(
            name = "Ethiopia Yirgacheffe",
            roaster = "Acme Roasters",
            origin = "Ethiopia",
            roastLevel = RoastLevel.LIGHT,
            process = Process.WASHED,
            weightGrams = 250,
            notes = "blueberry, floral",
        )
        val service = AiExtractionService(BeanExtractionRunner { canned })

        val result = runBlocking { service.extractFromImage(image, "image/jpeg") }

        assertNotNull(result)
        assertEquals("Ethiopia Yirgacheffe", result?.name)
        assertEquals(RoastLevel.LIGHT, result?.roastLevel)
        assertEquals(250, result?.weightGrams)
    }

    @Test
    fun `an all-null extraction is treated as no result`() {
        val service = AiExtractionService(BeanExtractionRunner { BeanExtraction() })

        val result = runBlocking { service.extractFromImage(image, "image/png") }

        assertNull(result, "An empty extraction must degrade to null so the form stays as-is")
    }

    @Test
    fun `a null from the runner degrades to null`() {
        val service = AiExtractionService(BeanExtractionRunner { null })

        assertNull(runBlocking { service.extractFromImage(image, "image/png") })
    }

    @Test
    fun `a failure in the runner falls back to null instead of throwing`() {
        val service = AiExtractionService(BeanExtractionRunner { error("LLM unavailable") })

        assertNull(runBlocking { service.extractFromImage(image, "image/webp") })
    }

    @Test
    fun `the built prompt carries a user message for the photo`() {
        var captured: Prompt? = null
        val service = AiExtractionService(
            BeanExtractionRunner { prompt -> captured = prompt; BeanExtraction(name = "X") },
        )

        runBlocking { service.extractFromImage(image, "image/jpeg") }

        assertNotNull(captured)
        assertTrue(
            captured!!.messages.any { it is Message.User },
            "The image prompt must include a user message",
        )
    }

    @Test
    fun `extractFromUrl returns the extraction from the fetched page`() {
        val canned = BeanExtraction(name = "Linked Bean", origin = "Peru")
        val service = AiExtractionService(
            runner = BeanExtractionRunner { canned },
            fetcher = { "<html><body>Linked Bean from Peru</body></html>" },
        )

        val result = runBlocking { service.extractFromUrl("https://roaster.example/linked-bean") }

        assertEquals("Linked Bean", result?.name)
        assertEquals("Peru", result?.origin)
    }

    @Test
    fun `extractFromUrl returns null and skips the model when the page cannot be fetched`() {
        var runnerCalled = false
        val service = AiExtractionService(
            runner = BeanExtractionRunner { runnerCalled = true; BeanExtraction(name = "X") },
            fetcher = { null },
        )

        val result = runBlocking { service.extractFromUrl("https://unreachable.example") }

        assertNull(result)
        assertFalse(runnerCalled, "No point calling the model when the page didn't load")
    }

    @Test
    fun `extractFromUrl returns null when the page reduces to no usable text`() {
        var runnerCalled = false
        val service = AiExtractionService(
            runner = BeanExtractionRunner { runnerCalled = true; BeanExtraction(name = "X") },
            fetcher = { "<script>var x = 1;</script>" },
        )

        val result = runBlocking { service.extractFromUrl("https://empty.example") }

        assertNull(result)
        assertFalse(runnerCalled, "Empty page text should not reach the model")
    }
}
