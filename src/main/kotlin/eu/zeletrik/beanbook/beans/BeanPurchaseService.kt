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

    @Transactional(readOnly = true)
    fun findAll(): List<BeanPurchase> = repository.findAll()

    /** The distinct tags across all purchases, sorted — for tag pickers and the filter dialog. */
    @Transactional(readOnly = true)
    fun allTags(): SortedSet<String> = repository.findAll().flatMapTo(sortedSetOf()) { it.tags }

    /** The distinct roaster names across all purchases, sorted — for the roaster typeahead. */
    @Transactional(readOnly = true)
    fun allRoasters(): SortedSet<String> = repository.findAll().mapTo(sortedSetOf()) { it.roaster }

    @Transactional
    fun save(purchase: BeanPurchase): BeanPurchase {
        // App-assigned UUIDs: choose INSERT vs UPDATE by existence, since Spring Data would
        // otherwise always UPDATE (the @Id is never null). See BeanPurchase.isNew / markNew.
        purchase.markNew(!repository.existsById(purchase.id))
        return repository.save(purchase)
    }

    @Transactional
    fun delete(id: UUID) = repository.deleteById(id)
}
