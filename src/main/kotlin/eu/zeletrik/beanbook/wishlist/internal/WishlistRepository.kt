package eu.zeletrik.beanbook.wishlist.internal

import eu.zeletrik.beanbook.wishlist.WishlistItem
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository
import java.util.UUID

/** Spring Data JDBC repository providing CRUD access to [WishlistItem] records. */
interface WishlistRepository : ListCrudRepository<WishlistItem, UUID> {
    /** Insertion order. With proper INSERT/UPDATE (no INSERT-OR-REPLACE) the rowid is stable across edits. */
    @Query("SELECT * FROM wishlist_items ORDER BY rowid")
    fun findAllByRowid(): List<WishlistItem>
}
