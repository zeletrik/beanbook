package eu.zeletrik.beanbook.ai.internal

import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import eu.zeletrik.beanbook.ai.AiExtractionService
import eu.zeletrik.beanbook.ai.BeanExtraction
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires the Koog [PromptExecutor] used for AI-assisted bean entry.
 *
 * The executor bean is created only when `beanbook.ai.enabled=true`, so the default offline
 * deployment never touches the network or requires credentials. Koog's own Spring Boot starter
 * targets Spring Boot 3.5, so the executor is built by hand here against Koog core.
 */
@Configuration
@EnableConfigurationProperties(AiProperties::class)
@ConditionalOnBooleanProperty("beanbook.ai.enabled")
class AiConfiguration {

    /**
     * Builds an OpenAI-backed prompt executor. Fails fast if the feature is enabled without an API
     * key, rather than surfacing the problem later mid-request.
     */
    @Bean
    fun promptExecutor(properties: AiProperties): PromptExecutor {
        val apiKey = properties.openai.apiKey?.takeIf { it.isNotBlank() }
            ?: error("beanbook.ai.enabled=true but no OpenAI API key found (set OPENAI_API_KEY)")
        return MultiLLMPromptExecutor(mapOf(LLMProvider.OpenAI to OpenAILLMClient(apiKey = apiKey)))
    }

    /**
     * The bean-extraction service, backed by Koog's structured executor. Gated by the same flag as the
     * executor, so it appears and disappears together with it; the `ui` module treats its absence as
     * "feature off" and hides the AI affordances.
     */
    @Bean
    @ConditionalOnBooleanProperty("beanbook.ai.enabled")
    fun aiExtractionService(executor: PromptExecutor, properties: AiProperties): AiExtractionService {
        val model = resolveModel(properties.openai.model)
        return AiExtractionService(
            { prompt -> executor.executeStructured<BeanExtraction>(prompt, model).getOrNull()?.data }
        )
    }

    /** Maps the configured model name to a Koog [LLModel] constant, defaulting to GPT-4o. */
    private fun resolveModel(model: AiProperties.OpenAi.Model): LLModel = when (model) {
        AiProperties.OpenAi.Model.GPT4o -> OpenAIModels.Chat.GPT4o
        AiProperties.OpenAi.Model.GPT4oMini -> OpenAIModels.Chat.GPT4oMini
        AiProperties.OpenAi.Model.GPT5 -> OpenAIModels.Chat.GPT5
    }
}
