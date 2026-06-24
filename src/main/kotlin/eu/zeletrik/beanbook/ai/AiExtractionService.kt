package eu.zeletrik.beanbook.ai

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import eu.zeletrik.beanbook.ai.internal.BeanExtractionRunner
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Extracts coffee-bean fields from a bag photo so the Add form can be pre-filled.
 *
 * Constructed by `AiConfiguration` only when the feature is enabled. Every failure mode — refusal,
 * unparseable output, network error, or timeout — degrades to `null` so the caller falls back to manual
 * entry; the service never throws (except on genuine coroutine cancellation, which is propagated).
 */
class AiExtractionService internal constructor(
    private val runner: BeanExtractionRunner,
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
    }
}
