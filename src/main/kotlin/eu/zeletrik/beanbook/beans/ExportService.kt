package eu.zeletrik.beanbook.beans

import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

data class ExportPayload(
    val purchases: List<BeanPurchase>,
    val wishlist: List<WishlistItem>,
)

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
