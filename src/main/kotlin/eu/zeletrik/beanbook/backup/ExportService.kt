package eu.zeletrik.beanbook.backup

import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

/** Serializable snapshot of all user data bundled into a single backup export. */
data class ExportPayload(
    val purchases: List<BeanPurchase>,
    val wishlist: List<WishlistItem>,
)

/** Builds a backup of all purchases and wishlist items as a JSON [ExportPayload]. */
@Service
class ExportService(
    private val beanPurchaseService: BeanPurchaseService,
    private val wishlistService: WishlistService,
    private val objectMapper: ObjectMapper,
) {
    fun generateJson(): ByteArray = objectMapper.writeValueAsBytes(
        ExportPayload(
            purchases = beanPurchaseService.findAll(),
            wishlist = wishlistService.findAll(),
        )
    )
}
