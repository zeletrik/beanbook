package eu.zeletrik.beanbook

import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.internal.WishlistRepository
import java.util.Optional
import java.util.UUID

/** In-memory [WishlistRepository] for unit tests. save() upserts by id; findAllByRowid() returns insertion order. */
class TestWishlistRepository : WishlistRepository {
    val store = mutableListOf<WishlistItem>()

    override fun findAllByRowid(): List<WishlistItem> = store.toList()

    override fun <S : WishlistItem> save(entity: S): S {
        val i = store.indexOfFirst { it.id == entity.id }
        if (i >= 0) store[i] = entity else store.add(entity)
        return entity
    }

    override fun <S : WishlistItem> saveAll(entities: Iterable<S>): List<S> = entities.map { save(it) }

    override fun findAll(): List<WishlistItem> = store.toList()
    override fun findAllById(ids: Iterable<UUID>): List<WishlistItem> =
        ids.mapNotNull { id -> store.firstOrNull { it.id == id } }

    override fun findById(id: UUID): Optional<WishlistItem> =
        Optional.ofNullable(store.firstOrNull { it.id == id })

    override fun existsById(id: UUID): Boolean = store.any { it.id == id }
    override fun count(): Long = store.size.toLong()

    override fun deleteById(id: UUID) { store.removeIf { it.id == id } }
    override fun delete(entity: WishlistItem) = deleteById(entity.id)
    override fun deleteAllById(ids: Iterable<UUID>) = ids.forEach { deleteById(it) }
    override fun deleteAll(entities: Iterable<WishlistItem>) = entities.forEach { delete(it) }
    override fun deleteAll() = store.clear()
}
