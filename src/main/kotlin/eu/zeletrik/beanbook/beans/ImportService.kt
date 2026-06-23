package eu.zeletrik.beanbook.beans

import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Service

data class ImportResult(val purchases: Int, val wishlist: Int, val skipped: Int, val success: Boolean = true) {
    companion object {
        val FAILURE = ImportResult(0, 0, 0, success = false)
    }
}

@Service
class ImportService(
    private val beanPurchaseService: BeanPurchaseService,
    private val wishlistService: WishlistService,
    private val objectMapper: ObjectMapper,
) {

    fun import(bytes: ByteArray): ImportResult {
        val root: JsonNode = try {
            objectMapper.readTree(bytes)
        } catch (e: Exception) {
            return ImportResult.FAILURE
        }

        if (bytes.isEmpty() || root.isNull) return ImportResult.FAILURE

        val purchaseNodes: Iterable<JsonNode>
        val wishlistNodes: Iterable<JsonNode>

        when {
            root.isArray -> {
                purchaseNodes = root
                wishlistNodes = emptyList()
            }
            root.isObject -> {
                val pNode = root.path("purchases")
                val wNode = root.path("wishlist")
                purchaseNodes = if (pNode.isMissingNode || pNode.isNull) emptyList() else pNode
                wishlistNodes = if (wNode.isMissingNode || wNode.isNull) emptyList() else wNode
            }
            else -> return ImportResult.FAILURE
        }

        var purchaseCount = 0
        var wishlistCount = 0
        var skipped = 0

        for (node in purchaseNodes) {
            try {
                // Default missing roastProfile to OMNI so legacy exports (pre-roastProfile feature)
                // import successfully instead of being skipped (Risk Hotspot 1).
                val patchedNode = if (node is ObjectNode && !node.has("roastProfile")) {
                    (node.deepCopy() as ObjectNode).apply { put("roastProfile", "OMNI") }
                } else node
                val purchase = objectMapper.treeToValue(patchedNode, BeanPurchase::class.java)
                beanPurchaseService.save(purchase)
                purchaseCount++
            } catch (e: Exception) {
                skipped++
            }
        }

        for (node in wishlistNodes) {
            try {
                val item = objectMapper.treeToValue(node, WishlistItem::class.java)
                wishlistService.upsert(item)
                wishlistCount++
            } catch (e: Exception) {
                skipped++
            }
        }

        return ImportResult(purchaseCount, wishlistCount, skipped)
    }
}
