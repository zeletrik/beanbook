package eu.zeletrik.beanbook.beans

/**
 * What an OMNI bean is actually brewed as. Modelled separately from [RoastProfile] — whose OMNI
 * value is meaningless as a brew target — so that the impossible `usedAs = OMNI` state cannot exist.
 */
enum class BrewTarget { ESPRESSO, FILTER }
