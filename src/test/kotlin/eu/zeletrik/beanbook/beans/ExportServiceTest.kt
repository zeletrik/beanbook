package eu.zeletrik.beanbook.beans

import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.TestWishlistRepository
import eu.zeletrik.beanbook.backup.ExportService
import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

/**
 * Verifies that [ExportService] serialises purchases and wishlist items into the expected JSON structure.
 */
class ExportServiceTest {

    private val objectMapper = jacksonObjectMapper()

    /**
     * Stub [WishlistService] backed by an in-memory list.
     *
     * [WishlistService] is open (kotlin-spring plugin). `JdbcTemplate()` (no `DataSource`)
     * is safe since all methods are overridden and never call super.
     */
    private fun wishlistService(items: List<WishlistItem> = emptyList()): WishlistService =
        object : WishlistService(TestWishlistRepository()) {
            private val store = items.toMutableList()
            override fun findAll() = store.toList()
            override fun upsert(item: WishlistItem) { store.removeIf { it.id == item.id }; store.add(item) }
            override fun deleteById(id: UUID) { store.removeIf { it.id == id } }
        }

    private fun makeService(
        vararg beans: BeanPurchase,
        wishlistItems: List<WishlistItem> = emptyList(),
    ): ExportService {
        val repo = object : TestBeanPurchaseRepository() { init { store.addAll(beans.toList()) } }
        val service = BeanPurchaseService(repo)
        return ExportService(service, wishlistService(wishlistItems), objectMapper)
    }

    private fun purchase(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Bean",
        imageData: ByteArray? = null,
    ) = BeanPurchase(
        id = id, name = name, roaster = "Roaster", origin = "Ethiopia",
        price = BigDecimal("18.50"), weightGrams = 250,
        purchaseDate = LocalDate.of(2025, 1, 15), roastDate = LocalDate.of(2025, 1, 10),
        roastLevel = RoastLevel.LIGHT, process = Process.NATURAL,
        roastProfile = eu.zeletrik.beanbook.beans.RoastProfile.FILTER,
        imageData = imageData,
    )

    // AC-27 / AC-26: empty export produces object with purchases and wishlist keys (not "[]")
    @Test
    fun `empty export produces object with purchases and wishlist keys`() {
        val json = String(makeService().generateJson())
        assertTrue(json.contains("\"purchases\""), "Must contain purchases key")
        assertTrue(json.contains("\"wishlist\""), "Must contain wishlist key")
        assertTrue(json.contains("\"purchases\":[]") || json.contains("\"purchases\": []"),
            "Purchases must be empty array")
        assertTrue(json.contains("\"wishlist\":[]") || json.contains("\"wishlist\": []"),
            "Wishlist must be empty array")
    }

    // AC-25: non-empty wishlist appears under wishlist key
    @Test
    fun `wishlist items appear under wishlist key`() {
        val item = WishlistItem(UUID.randomUUID(), "Wish Bean", "Roaster", "Kenya")
        val json = String(makeService(wishlistItems = listOf(item)).generateJson())
        assertTrue(json.contains("Wish Bean"), "Wishlist item name must appear in export")
        assertTrue(json.contains("\"wishlist\""), "wishlist key must be present")
    }

    // AC-14 / AC-27: all bean fields present under purchases key
    @Test
    fun `exported JSON contains all bean fields under purchases key`() {
        val id = UUID.randomUUID()
        val json = String(makeService(purchase(id = id, name = "Field Check")).generateJson())
        assertTrue(json.contains("\"purchases\""), "purchases key must be present")
        assertTrue(json.contains(id.toString()), "id must be in JSON")
        assertTrue(json.contains("Field Check"), "name must be in JSON")
        assertTrue(json.contains("\"roastProfile\""), "roastProfile key must be present")
        assertTrue(json.contains("\"usedAs\""), "usedAs key must be present")
    }

    // AC-15: non-null imageData serialises as base64 under purchases
    @Test
    fun `bean with image data produces non-empty base64 string under purchases`() {
        val bytes = ByteArray(64) { it.toByte() }
        val expectedBase64 = Base64.getEncoder().encodeToString(bytes)
        val json = String(makeService(purchase(imageData = bytes)).generateJson())
        assertTrue(json.contains(expectedBase64), "Expected base64 image data in JSON")
    }

    // AC-16: null imageData serialises as JSON null
    @Test
    fun `bean without image data produces null imageData`() {
        val json = String(makeService(purchase(imageData = null)).generateJson())
        assertTrue(json.contains("\"imageData\":null") || json.contains("\"imageData\": null"),
            "imageData must be null in JSON when no image")
    }
}
