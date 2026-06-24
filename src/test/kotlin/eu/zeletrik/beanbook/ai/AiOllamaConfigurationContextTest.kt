package eu.zeletrik.beanbook.ai

import ai.koog.prompt.executor.model.PromptExecutor
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * With the AI feature enabled and the Ollama provider selected, the executor and service must wire into
 * the context with no API key and without contacting the Ollama server — the client connects lazily on
 * first request, so startup needs no running server.
 */
@SpringBootTest(
    properties = [
        "beanbook.ai.enabled=true",
        "beanbook.ai.provider=OLLAMA",
    ],
)
class AiOllamaConfigurationContextTest(
    @Autowired private val promptExecutor: PromptExecutor?,
    @Autowired private val aiExtractionService: AiExtractionService?,
) {

    @Test
    fun `the AI beans wire up for the Ollama provider without a key or a running server`() {
        assertNotNull(promptExecutor, "PromptExecutor bean must exist for provider=OLLAMA")
        assertNotNull(aiExtractionService, "AiExtractionService bean must exist for provider=OLLAMA")
    }
}
