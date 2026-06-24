package eu.zeletrik.beanbook.ai

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import eu.zeletrik.beanbook.ai.internal.BeanExtractionRunner
import eu.zeletrik.beanbook.ai.internal.PageFetcher
import eu.zeletrik.beanbook.ai.internal.ProductPageFetcher
import eu.zeletrik.beanbook.ai.internal.reduceHtml
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Extracts coffee-bean fields from a bag photo or a product/roaster URL so the Add form can be pre-filled.
 *
 * Constructed by `AiConfiguration` only when the feature is enabled. Every failure mode — refusal,
 * unparseable output, network error, or timeout — degrades to `null` so the caller falls back to manual
 * entry; the service never throws (except on genuine coroutine cancellation, which is propagated).
 */
class AiExtractionService internal constructor(
    private val runner: BeanExtractionRunner,
    private val fetcher: PageFetcher = ProductPageFetcher(),
    private val timeout: Duration = DEFAULT_TIMEOUT,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Sends [imageBytes] (a JPEG/PNG/WebP bag photo, identified by [mimeType]) to a vision-capable model
     * and returns the extracted fields, or `null` if nothing usable could be read.
     */
    suspend fun extractFromImage(imageBytes: ByteArray, mimeType: String): BeanExtraction? {
        val prompt = buildImagePrompt(imageBytes, mimeType)
        val result = guarded { runner.run(prompt) }
        return result?.takeUnless { it.isEmpty() }
    }

    /**
     * Fetches [url], reduces the page to text, and asks the model for the bean fields. Returns `null` if
     * the page can't be fetched or nothing usable could be read.
     */
    suspend fun extractFromUrl(url: String): BeanExtraction? {
        val html = fetcher.fetch(url) ?: return null
        val pageText = reduceHtml(html)
        if (pageText.isBlank()) return null
        val result = guarded { runner.run(buildUrlPrompt(pageText)) }
        return result?.takeUnless { it.isEmpty() }
    }

    private fun buildUrlPrompt(pageText: String) =
        prompt("extract-bean-from-url") {
            system(URL_SYSTEM_PROMPT)
            user("$URL_USER_INSTRUCTION\n\n$pageText")
        }

    private fun buildImagePrompt(imageBytes: ByteArray, mimeType: String) =
        prompt("extract-bean-from-photo") {
            system(SYSTEM_PROMPT)
            user {
                text(USER_INSTRUCTION)
                val subtype = mimeType.substringAfter('/', "jpeg")
                image(
                    AttachmentSource.Image(
                        content = AttachmentContent.Binary.Bytes(imageBytes),
                        format = subtype,
                        mimeType = mimeType,
                        fileName = "bag.$subtype",
                    ),
                )
            }
        }

    /** Runs [block] under the timeout, mapping every failure to `null`; re-throws genuine cancellation. */
    private suspend fun guarded(block: suspend () -> BeanExtraction?): BeanExtraction? =
        try {
            withTimeoutOrNull(timeout) { block() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("AI extraction failed; falling back to manual entry", e)
            null
        }

    companion object {
        private val DEFAULT_TIMEOUT = 30.seconds

        private const val SYSTEM_PROMPT =
            "You extract structured details from a photo of a coffee-bean bag. Read only what is clearly " +
                "printed on the bag. For any field you cannot read with confidence, return null — never " +
                "guess or infer. When a weight is given in ounces, convert it to grams (1 oz ≈ 28.35 g)."

        private const val USER_INSTRUCTION = "Extract the coffee-bean details from this bag photo."

        private const val URL_SYSTEM_PROMPT =
            "You extract structured details from the text of a coffee product or roaster web page. Use " +
                "only what the text states; for anything not present, return null — never guess. When a " +
                "weight is given in ounces, convert it to grams (1 oz ≈ 28.35 g)."

        private const val URL_USER_INSTRUCTION =
            "Extract the coffee-bean details from this product page text:"
    }
}
