package eu.zeletrik.beanbook.wishlist

import eu.zeletrik.beanbook.wishlist.internal.WishlistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** Transactional access to wishlist entries, persisting [WishlistItem]s with app-assigned ids. */
@Service
class WishlistService(private val repository: WishlistRepository) {

    @Transactional(readOnly = true)
    fun findAll(): List<WishlistItem> = repository.findAllByRowid()

    /** Inserts [item] if its id is new, otherwise updates the existing row in place. */
    @Transactional
    fun upsert(item: WishlistItem) {
        // App-assigned UUID: choose INSERT vs UPDATE by existence (a proper UPDATE preserves the
        // rowid, so insertion order survives edits — unlike the old INSERT OR REPLACE).
        item.markNew(!repository.existsById(item.id))
        repository.save(item)
    }

    @Transactional
    fun deleteById(id: UUID) {
        repository.deleteById(id)
    }
}
