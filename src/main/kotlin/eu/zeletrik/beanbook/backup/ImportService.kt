package eu.zeletrik.beanbook.backup

import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Outcome of an import: the number of persisted [purchases] and [wishlist] items, how many records were [skipped] as
 * unparseable, and whether the import overall succeeded.
 */
data class ImportResult(
    val purchases: Int,
    val wishlist: Int,
    val skipped: Int,
    val success: Boolean = true,
    /** Short, user-facing reason when [success] is `false` (e.g. "not valid JSON"); null on success. */
    val error: String? = null,
) {
    companion object {
        /** Sentinel result for a failed import (e.g. unparseable input); reports zero records and [success] `false`. */
        val FAILURE = ImportResult(0, 0, 0, success = false)

        /** A failed import carrying a short [reason] to surface to the user. */
        fun failure(reason: String) = ImportResult(0, 0, 0, success = false, error = reason)
    }
}

/** Restores bean purchases and wishlist items from an exported JSON backup, upserting them by id. */
@Service
class ImportService(
    private val beanPurchaseService: BeanPurchaseService,
    private val wishlistService: WishlistService,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Parses a JSON backup (either a bare purchases array or an object with `purchases`/`wishlist` arrays) and persists
     * the records. Unparseable records are skipped and counted; the whole operation is transactional, so a persistence
     * failure rolls back every write.
     *
     * @return an [ImportResult] with the persisted and skipped counts, or [ImportResult.FAILURE] on invalid input.
     */
    @Transactional
    fun import(bytes: ByteArray): ImportResult {
        val root: JsonNode = try {
            objectMapper.readTree(bytes)
        } catch (e: Exception) {
            log.warn("Import failed: could not parse JSON", e)
            return ImportResult.failure("the file isn't valid JSON")
        }

        if (bytes.isEmpty() || root.isNull) return ImportResult.failure("the file is empty")

        val purchaseNodes: Iterable<JsonNode>
        val wishlistNodes: Iterable<JsonNode>

        when {
            root.isArray -> {
                purchaseNodes = root
                wishlistNodes = emptyList()
            }
            root.isObject -> {
                // Require array nodes: a present-but-non-array "purchases"/"wishlist" (e.g. an
                // object) must not be iterated as records — treat it as "no records of that kind".
                purchaseNodes = root.path("purchases").takeIf { it.isArray } ?: emptyList()
                wishlistNodes = root.path("wishlist").takeIf { it.isArray } ?: emptyList()
            }
            else -> return ImportResult.failure("expected a purchases array or a backup object")
        }

        var skipped = 0

        // Parse phase (no DB writes): unparseable records are skipped and counted.
        val purchases = purchaseNodes.mapNotNull { node ->
            try {
                // Default missing roastProfile to OMNI so legacy exports (pre-roastProfile feature)
                // import successfully instead of being skipped (Risk Hotspot 1). Mutate the node
                // in place — it is parsed once and discarded, so cloning the (possibly large,
                // base64-image-bearing) node would be wasteful.
                if (node is ObjectNode && !node.has("roastProfile")) {
                    node.put("roastProfile", "OMNI")
                }
                // Normalise tags at the import boundary (the interactive form does this too): a tag
                // containing a comma would otherwise be silently split into two when the CSV tags
                // column is read back. Splitting here makes the round-trip deterministic.
                val parsed = objectMapper.treeToValue(node, BeanPurchase::class.java)
                parsed.copy(tags = normaliseTags(parsed.tags))
            } catch (e: Exception) {
                log.warn("Skipping unparseable purchase record during import", e)
                skipped++
                null
            }
        }
        val items = wishlistNodes.mapNotNull { node ->
            try {
                objectMapper.treeToValue(node, WishlistItem::class.java)
            } catch (e: Exception) {
                log.warn("Skipping unparseable wishlist record during import", e)
                skipped++
                null
            }
        }

        // Collapse duplicate ids within the file so the reported counts match the rows actually
        // persisted (save/upsert are id-keyed upserts, so two same-id records yield one row).
        val purchasesById = purchases.associateBy { it.id }.values
        val itemsById = items.associateBy { it.id }.values

        // Save phase: all-or-nothing within the @Transactional boundary. A persistence failure
        // here rolls the whole import back rather than leaving the database half-applied.
        purchasesById.forEach { beanPurchaseService.save(it) }
        itemsById.forEach { wishlistService.upsert(it) }

        return ImportResult(purchasesById.size, itemsById.size, skipped)
    }

    private fun normaliseTags(tags: Set<String>): Set<String> =
        tags.flatMap { it.split(",") }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toCollection(LinkedHashSet())
}
