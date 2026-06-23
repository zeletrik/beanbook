package eu.zeletrik.beanbook

import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/**
 * In-memory WishlistService stub for Karibu / unit tests.
 * kotlin-spring makes WishlistService open.
 */
class TestWishlistService : WishlistService(JdbcTemplate()) {
    val store = mutableListOf<WishlistItem>()
    override fun findAll() = store.toList()
    override fun upsert(item: WishlistItem) { store.removeIf { it.id == item.id }; store.add(item) }
    override fun deleteById(id: UUID) { store.removeIf { it.id == id } }
}
