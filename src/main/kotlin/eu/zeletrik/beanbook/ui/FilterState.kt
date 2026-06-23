package eu.zeletrik.beanbook.ui

import eu.zeletrik.beanbook.beans.BagState
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel

enum class SortField(val label: String) {
    PURCHASE_DATE("Date"),
    NAME("Name"),
    ROASTER("Roaster"),
    PRICE("Price"),
    RATING("Rating"),
}

data class FilterState(
    val query: String = "",
    val roastLevels: Set<RoastLevel> = emptySet(),
    val processes: Set<Process> = emptySet(),
    val bagStates: Set<BagState> = emptySet(),
    val minRating: Int? = null,
    val tags: Set<String> = emptySet(),
    val sortBy: SortField = SortField.PURCHASE_DATE,
    val ascending: Boolean = false,
) {
    val activeFilterCount: Int
        get() = (if (roastLevels.isNotEmpty()) 1 else 0) +
                (if (processes.isNotEmpty()) 1 else 0) +
                (if (bagStates.isNotEmpty()) 1 else 0) +
                (if (minRating != null) 1 else 0) +
                (if (tags.isNotEmpty()) 1 else 0)

    val isDefault: Boolean get() = activeFilterCount == 0 && sortBy == SortField.PURCHASE_DATE && !ascending
}

private fun SortField.toComparator(): Comparator<BeanPurchase> = when (this) {
    SortField.NAME          -> compareBy { it.name }
    SortField.ROASTER       -> compareBy { it.roaster }
    SortField.PRICE         -> compareBy { it.pricePerUnit }
    SortField.PURCHASE_DATE -> compareBy { it.purchaseDate }
    SortField.RATING        -> compareBy { it.rating ?: 0 }
}

fun List<BeanPurchase>.applyFilter(state: FilterState): List<BeanPurchase> {
    val q = state.query.trim().lowercase()
    val comparator = state.sortBy.toComparator().let { if (state.ascending) it else it.reversed() }
    return this
        .filter { p ->
            q.isEmpty() ||
                p.name.lowercase().contains(q) ||
                p.roaster.lowercase().contains(q) ||
                p.origin.lowercase().contains(q) ||
                p.tags.any { tag -> tag.contains(q, ignoreCase = true) }
        }
        .filter { p -> state.roastLevels.isEmpty() || p.roastLevel in state.roastLevels }
        .filter { p -> state.processes.isEmpty() || p.process in state.processes }
        .filter { p -> state.bagStates.isEmpty() || p.bagState in state.bagStates }
        .filter { p -> state.minRating == null || (p.rating != null && p.rating >= state.minRating) }
        .filter { p -> state.tags.isEmpty() || p.tags.any { it in state.tags } }
        .sortedWith(comparator)
}
