package eu.zeletrik.beanbook.beans

import kotlinx.serialization.Serializable

/** Roast darkness of a coffee bean. `@Serializable` so the `ai` module can use it in LLM-extracted DTOs. */
@Serializable
enum class RoastLevel {
    LIGHT, MEDIUM, DARK
}
