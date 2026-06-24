package eu.zeletrik.beanbook.ai.internal

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for the (optional, off-by-default) AI-assisted bean entry feature, bound from the
 * `beanbook.ai.*` namespace.
 *
 * The OpenAI API key is supplied via the `OPENAI_API_KEY` environment variable (wired through
 * `application.yml`) and is never committed or baked into the Docker image.
 */
@ConfigurationProperties(prefix = "beanbook.ai")
data class AiProperties(
    /** Master switch; when `false` no LLM client is created and the feature is invisible. */
    val enabled: Boolean = false,
    /** Which backend to use. OpenAI today; local models (e.g. Mistral via Ollama) later. */
    val provider: AiProvider = AiProvider.OPENAI,
    val openai: OpenAi = OpenAi(),
) {
    /** OpenAI-specific settings. */
    data class OpenAi(
        /** API key, injected from the `OPENAI_API_KEY` environment variable; `null`/blank when unset. */
        val apiKey: String? = null,
        /** Vision-capable model used for extraction. */
        val model: String = "gpt-4o",
    )
}

/** Supported AI backends. Only [OPENAI] is wired today; [OLLAMA] is reserved for local inference. */
enum class AiProvider { OPENAI, OLLAMA }
