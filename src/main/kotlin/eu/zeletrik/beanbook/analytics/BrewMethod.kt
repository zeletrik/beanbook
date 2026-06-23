package eu.zeletrik.beanbook.analytics

import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.RoastProfile

enum class BrewMethod { ESPRESSO, FILTER, UNCLASSIFIED }

fun BeanPurchase.effectiveBrewMethod(): BrewMethod = when {
    roastProfile == RoastProfile.ESPRESSO -> BrewMethod.ESPRESSO
    roastProfile == RoastProfile.FILTER   -> BrewMethod.FILTER
    roastProfile == RoastProfile.OMNI && usedAs == RoastProfile.ESPRESSO -> BrewMethod.ESPRESSO
    roastProfile == RoastProfile.OMNI && usedAs == RoastProfile.FILTER   -> BrewMethod.FILTER
    else -> BrewMethod.UNCLASSIFIED
}
