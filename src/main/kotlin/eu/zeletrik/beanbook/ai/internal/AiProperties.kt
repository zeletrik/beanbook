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
    val ollama: Ollama = Ollama(),
) {
    /** OpenAI-specific settings. */
    data class OpenAi(
        /** API key, injected from the `OPENAI_API_KEY` environment variable; `null`/blank when unset. */
        val apiKey: String? = null,
        /** Vision-capable model used for extraction. */
        val model: Model = Model.GPT4o,
    ) {
        enum class Model { GPT4o, GPT4oMini, GPT5 }
    }

    /** Ollama (local, on-box inference) settings. */
    data class Ollama(
        /** Base URL of the local Ollama server. */
        val baseUrl: String = "http://localhost:11434",
        /** Local model to use; [Model.GRANITE_VISION] is multimodal and required for the photo path. */
        val model: Model = Model.GRANITE_VISION,
    ) {
        enum class Model { GRANITE_VISION, LLAMA_3_2 }
    }
}

/** Supported AI backends. Only [OPENAI] is wired today; [OLLAMA] is reserved for local inference. */
enum class AiProvider { OPENAI, OLLAMA }
