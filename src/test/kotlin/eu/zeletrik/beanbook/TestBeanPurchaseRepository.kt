package eu.zeletrik.beanbook

import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.internal.BeanPurchaseRepository
import java.util.Optional
import java.util.UUID

/**
 * In-memory [BeanPurchaseRepository] for unit tests. save() upserts by id, so it serves both
 * inserts and updates regardless of the entity's isNew flag.
 * Construct the service with: BeanPurchaseService(repo).
 */
abstract class TestBeanPurchaseRepository : BeanPurchaseRepository {
    val store = mutableListOf<BeanPurchase>()

    @Suppress("UNCHECKED_CAST")
    override fun <S : BeanPurchase> save(entity: S): S {
        val i = store.indexOfFirst { it.id == entity.id }
        if (i >= 0) store[i] = entity else store.add(entity)
        return entity
    }

    @Suppress("UNCHECKED_CAST")
    override fun <S : BeanPurchase> saveAll(entities: Iterable<S>): List<S> =
        entities.map { save(it) }

    override fun findAll(): List<BeanPurchase> = store.toList()
    override fun findAllById(ids: Iterable<UUID>): List<BeanPurchase> =
        ids.mapNotNull { id -> store.firstOrNull { it.id == id } }

    override fun findById(id: UUID): Optional<BeanPurchase> =
        Optional.ofNullable(store.firstOrNull { it.id == id })

    override fun existsById(id: UUID): Boolean = store.any { it.id == id }
    override fun count(): Long = store.size.toLong()

    override fun deleteById(id: UUID) { store.removeIf { it.id == id } }
    override fun delete(entity: BeanPurchase) = deleteById(entity.id)
    override fun deleteAllById(ids: Iterable<UUID>) = ids.forEach { deleteById(it) }
    override fun deleteAll(entities: Iterable<BeanPurchase>) = entities.forEach { delete(it) }
    override fun deleteAll() = store.clear()
}
