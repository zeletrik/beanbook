package eu.zeletrik.beanbook.beans

import eu.zeletrik.beanbook.beans.internal.BeanPurchaseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.SortedSet
import java.util.UUID

/** Transactional service for reading and persisting [BeanPurchase] entities and their derived data. */
@Service
class BeanPurchaseService(
    private val repository: BeanPurchaseRepository,
) {

    private companion object {
        private val WHITESPACE = Regex("\\s+")
    }

    @Transactional(readOnly = true)
    fun findAll(): List<BeanPurchase> = repository.findAll()

    /** The distinct tags across all purchases, sorted — for tag pickers and the filter dialog. */
    @Transactional(readOnly = true)
    fun allTags(): SortedSet<String> = repository.findAll().flatMapTo(sortedSetOf()) { it.tags }

    /**
     * The distinct roaster names across all purchases — for the roaster typeahead. Compared
     * case-insensitively so pre-existing case variants ("Onyx" / "onyx") collapse to one suggestion.
     */
    @Transactional(readOnly = true)
    fun allRoasters(): SortedSet<String> =
        repository.findAll().mapTo(sortedSetOf(String.CASE_INSENSITIVE_ORDER)) { it.roaster }

    @Transactional
    fun save(purchase: BeanPurchase): BeanPurchase {
        // Normalize free-text identity fields so casing/whitespace differences don't fork one roaster,
        // origin, or region into several. Compare against the OTHER purchases (exclude self) so editing
        // the sole holder of a value can still re-case it. Every write — create, edit, duplicate, and
        // import — funnels through here, so this is the one place normalization needs to live.
        val others = repository.findAll().filterNot { it.id == purchase.id }
        val normalized = purchase.copy(
            roaster = canonical(purchase.roaster, others.map { it.roaster }),
            origin = canonical(purchase.origin, others.map { it.origin }),
            region = purchase.region?.let { canonicalOrNull(it, others.mapNotNull(BeanPurchase::region)) },
        )
        // App-assigned UUIDs: choose INSERT vs UPDATE by existence, since Spring Data would
        // otherwise always UPDATE (the @Id is never null). See BeanPurchase.isNew / markNew.
        normalized.markNew(!repository.existsById(normalized.id))
        return repository.save(normalized)
    }

    /**
     * Trims and collapses internal whitespace, then — if the cleaned value matches one already in
     * [existing] ignoring case — reuses that spelling (first-seen casing wins), else keeps the cleaned
     * input. Preserves intentional brand casing (e.g. "DAK Coffee Roasters") for genuinely new values.
     */
    private fun canonical(value: String, existing: Collection<String>): String {
        val cleaned = value.trim().replace(WHITESPACE, " ")
        return existing.firstOrNull { it.equals(cleaned, ignoreCase = true) } ?: cleaned
    }

    /** Like [canonical], but a blank/whitespace-only value normalizes to `null` (region is optional). */
    private fun canonicalOrNull(value: String, existing: Collection<String>): String? =
        canonical(value, existing).takeIf { it.isNotEmpty() }

    @Transactional
    fun delete(id: UUID) = repository.deleteById(id)
}
