package eu.zeletrik.beanbook.beans

import kotlinx.serialization.Serializable

/** Coffee bean post-harvest processing method. `@Serializable` so the `ai` module can use it in LLM-extracted DTOs. */
@Serializable
enum class Process {
    WASHED, NATURAL, HONEY
}
