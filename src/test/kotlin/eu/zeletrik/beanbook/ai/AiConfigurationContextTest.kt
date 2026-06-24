package eu.zeletrik.beanbook.ai

import ai.koog.prompt.executor.model.PromptExecutor
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * With the AI feature enabled (dummy key, no network call), the Koog executor and the extraction
 * service must wire into the context. Guards against regressions in `AiConfiguration` that the
 * default flag-off tests can't catch.
 */
@SpringBootTest(
    properties = [
        "beanbook.ai.enabled=true",
        "beanbook.ai.openai.api-key=test-key-never-used",
    ],
)
class AiConfigurationContextTest(
    @Autowired private val promptExecutor: PromptExecutor?,
    @Autowired private val aiExtractionService: AiExtractionService?,
) {

    @Test
    fun `the AI beans are created when the feature is enabled`() {
        assertNotNull(promptExecutor, "PromptExecutor bean must exist when beanbook.ai.enabled=true")
        assertNotNull(aiExtractionService, "AiExtractionService bean must exist when beanbook.ai.enabled=true")
    }
}
