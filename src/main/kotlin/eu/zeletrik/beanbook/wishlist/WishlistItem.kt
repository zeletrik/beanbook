package eu.zeletrik.beanbook.wishlist

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

/**
 * A coffee bean the user wants to try, tracked separately from purchased
 * [eu.zeletrik.beanbook.beans.BeanPurchase] entries.
 */
@Table("wishlist_items")
data class WishlistItem(
    @Id
    @get:JvmName("idValue")
    val id: UUID,
    val name: String,
    val roaster: String = "",
    val origin: String = "",
    val notes: String? = null,
    /** Optional link to the bean's product page or the roaster's profile. */
    val url: String? = null,
) : Persistable<UUID> {

    /**
     * `id` is app-assigned, so (like [eu.zeletrik.beanbook.beans.BeanPurchase]) WishlistService drives insert-vs-update via this
     * flag rather than Spring Data's null-@Id heuristic. @get:JvmName frees the getId() slot.
     */
    override fun getId(): UUID = id

    @Transient
    private var newEntity: Boolean = true

    @Transient
    override fun isNew(): Boolean = newEntity

    /** Set by WishlistService before save() to choose INSERT (true) vs UPDATE (false). */
    fun markNew(isNew: Boolean): WishlistItem = apply { newEntity = isNew }
}
