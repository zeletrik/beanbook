package eu.zeletrik.beanbook.beans

import kotlinx.serialization.Serializable

/** Intended brewing/roast profile of a coffee bean. `@Serializable` so the `ai` module can use it in LLM-extracted DTOs. */
@Serializable
enum class RoastProfile {
    ESPRESSO, FILTER, OMNI
}
