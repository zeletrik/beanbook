package eu.zeletrik.beanbook.ai.internal

import ai.koog.http.client.HttpClientFactoryResolver
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
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
     * Builds the prompt executor for the configured provider. OpenAI fails fast if enabled without an API
     * key; Ollama talks to a local server (default `http://localhost:11434`) and needs no credentials.
     * The Ollama HTTP client reuses the same ServiceLoader-discovered [HttpClientFactoryResolver] factory
     * that the OpenAI client defaults to.
     */
    @Bean
    fun promptExecutor(properties: AiProperties): PromptExecutor = when (properties.provider) {
        AiProvider.OPENAI -> {
            val apiKey = properties.openai.apiKey?.takeIf { it.isNotBlank() }
                ?: error("beanbook.ai.enabled=true with provider=OPENAI but no API key found (set OPENAI_API_KEY)")
            MultiLLMPromptExecutor(mapOf(LLMProvider.OpenAI to OpenAILLMClient(apiKey = apiKey)))
        }
        AiProvider.OLLAMA -> MultiLLMPromptExecutor(
            mapOf(
                LLMProvider.Ollama to OllamaClient(
                    httpClientFactory = HttpClientFactoryResolver.resolve(),
                    baseUrl = properties.ollama.baseUrl,
                ),
            ),
        )
    }

    /**
     * The bean-extraction service, backed by Koog's structured executor. Gated by the same flag as the
     * executor, so it appears and disappears together with it; the `ui` module treats its absence as
     * "feature off" and hides the AI affordances.
     */
    @Bean
    @ConditionalOnBooleanProperty("beanbook.ai.enabled")
    fun aiExtractionService(executor: PromptExecutor, properties: AiProperties): AiExtractionService {
        val model = resolveModel(properties)
        return AiExtractionService(
            { prompt -> executor.executeStructured<BeanExtraction>(prompt, model).getOrNull()?.data }
        )
    }

    /** Resolves the configured provider + model to a Koog [LLModel] constant (with correct capabilities). */
    private fun resolveModel(properties: AiProperties): LLModel = when (properties.provider) {
        AiProvider.OPENAI -> when (properties.openai.model) {
            AiProperties.OpenAi.Model.GPT4o -> OpenAIModels.Chat.GPT4o
            AiProperties.OpenAi.Model.GPT4oMini -> OpenAIModels.Chat.GPT4oMini
            AiProperties.OpenAi.Model.GPT5 -> OpenAIModels.Chat.GPT5
        }
        AiProvider.OLLAMA -> when (properties.ollama.model) {
            AiProperties.Ollama.Model.GRANITE_VISION -> OllamaModels.Granite.GRANITE_3_2_VISION
            AiProperties.Ollama.Model.LLAMA_3_2 -> OllamaModels.Meta.LLAMA_3_2
        }
    }
}
