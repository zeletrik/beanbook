package eu.zeletrik.beanbook.wishlist

import java.util.UUID

data class WishlistItem(
    val id: UUID,
    val name: String,
    val roaster: String = "",
    val origin: String = "",
    val notes: String? = null,
)
