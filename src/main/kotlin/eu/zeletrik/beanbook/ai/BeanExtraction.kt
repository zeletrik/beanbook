package eu.zeletrik.beanbook.ai

import ai.koog.agents.core.tools.annotations.LLMDescription
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.RoastProfile
import kotlinx.serialization.Serializable

/**
 * Coffee-bean fields extracted by the LLM from a bag photo or product page, to pre-fill the Add form.
 *
 * Every field is nullable and defaults to `null`: the model is instructed to return `null` for anything
 * not clearly present rather than guess. The `@property:LLMDescription` annotations drive the JSON schema
 * Koog generates for structured output. The `ui` module maps this to a `PurchaseFormBean`, filling only
 * blank fields; rating, dates, photo, and tags are never AI-extracted (they are the user's own data).
 */
@Serializable
data class BeanExtraction(
    @property:LLMDescription(
        "Bean or blend name exactly as printed, e.g. \"Ethiopia Yirgacheffe\". Null if not visible."
    )
    val name: String? = null,
    @property:LLMDescription("Roaster or brand name. Null if not visible.")
    val roaster: String? = null,
    @property:LLMDescription("Country of origin, e.g. \"Colombia\". Null for a blend or if unclear.")
    val origin: String? = null,
    @property:LLMDescription("Region or sub-origin within the country, e.g. \"Huila\" or \"Yirgacheffe\". Null if not stated.")
    val region: String? = null,
    @property:LLMDescription("Roast level if stated; null if not clearly indicated.")
    val roastLevel: RoastLevel? = null,
    @property:LLMDescription("Processing method if stated; null if not clearly indicated.")
    val process: Process? = null,
    @property:LLMDescription(
        "Intended brew profile. If any 'espresso' or 'filter' badge is present, return it as a RoastProfile." +
                "Null if not indicated."
    )
    val roastProfile: RoastProfile? = null,
    @property:LLMDescription(
        "Net weight in grams; convert ounces (1 oz ≈ 28.35 g), e.g. \"250g\"->250, \"12oz\"->340. Null if absent."
    )
    val weightGrams: Int? = null,
    @property:LLMDescription(
        "Price as a plain decimal in the listed currency (product pages only; bags rarely show it). Null if absent."
    )
    val price: Double? = null,
    @property:LLMDescription(
        "Tasting notes printed on the bag or page, e.g. \"blueberry, chocolate, floral\". Null if absent."
    )
    val notes: String? = null,
    @property:LLMDescription(
        "Roast date if printed (often labelled \"Roasted on\" / \"Roast date\", " +
                "or if only one date present it's usually that, return that one)." +
                "Return it in ISO-8601 (yyyy-MM-dd) format or Null if not indicated." +
                "Do not confuse with a best-before / expiry date."
    )
    val roastDate: String? = null,
) {
    /** True when the model found nothing usable, so the caller can treat it as a non-result. */
    fun isEmpty(): Boolean = name == null && roaster == null && origin == null && roastLevel == null &&
            process == null && roastProfile == null && weightGrams == null && price == null && notes == null &&
            roastDate == null && region == null
}
