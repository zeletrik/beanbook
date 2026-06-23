package eu.zeletrik.beanbook.analytics

import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BrewTarget
import eu.zeletrik.beanbook.beans.RoastProfile

/** The resolved brewing method for a purchase, used for analytics grouping. */
enum class BrewMethod { ESPRESSO, FILTER, UNCLASSIFIED }

/**
 * Derives the [BrewMethod] for this purchase from its [BeanPurchase.roastProfile] and intended [BeanPurchase.usedAs].
 *
 * An omni roast resolves via its brew target; anything that cannot be classified returns [BrewMethod.UNCLASSIFIED].
 */
fun BeanPurchase.effectiveBrewMethod(): BrewMethod = when {
    roastProfile == RoastProfile.ESPRESSO -> BrewMethod.ESPRESSO
    roastProfile == RoastProfile.FILTER   -> BrewMethod.FILTER
    roastProfile == RoastProfile.OMNI && usedAs == BrewTarget.ESPRESSO -> BrewMethod.ESPRESSO
    roastProfile == RoastProfile.OMNI && usedAs == BrewTarget.FILTER   -> BrewMethod.FILTER
    else -> BrewMethod.UNCLASSIFIED
}
