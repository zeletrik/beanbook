package eu.zeletrik.beanbook.beans

import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class ImportServiceTest {

    private val objectMapper = jacksonObjectMapper()

    private val purchaseStore = mutableListOf<BeanPurchase>()
    private val wishlistStore = mutableListOf<WishlistItem>()

    private val beanRepo = object : TestBeanPurchaseRepository() { init { store.clear() } }
    private val beanService = BeanPurchaseService(beanRepo, beanRepo)
    private val wishlistService = object : WishlistService(JdbcTemplate()) {
        override fun findAll() = wishlistStore.toList()
        override fun upsert(item: WishlistItem) { wishlistStore.removeIf { it.id == item.id }; wishlistStore.add(item) }
        override fun deleteById(id: UUID) { wishlistStore.removeIf { it.id == id } }
    }
    private val service = ImportService(beanService, wishlistService, objectMapper)
    private val exportService = ExportService(beanService, wishlistService, objectMapper)

    private fun purchase(
        id: UUID = UUID.randomUUID(),
        name: String = "Test",
        roastProfile: RoastProfile = RoastProfile.FILTER,
    ) = BeanPurchase(
        id = id, name = name, roaster = "R", origin = "E",
        pricePerUnit = BigDecimal("15.00"), weightGrams = 250,
        purchaseDate = LocalDate.of(2025, 1, 1), roastDate = LocalDate.of(2024, 12, 28),
        roastLevel = RoastLevel.MEDIUM, process = Process.WASHED,
        roastProfile = roastProfile,
    )

    // AC-10: legacy plain-array format imported as purchases only
    @Test
    fun `legacy plain array imported as purchases`() {
        val p = purchase(name = "Legacy")
        val legacyJson = objectMapper.writeValueAsBytes(listOf(p))
        val result = service.import(legacyJson)
        assertTrue(result.success)
        assertEquals(1, result.purchases)
        assertEquals(0, result.wishlist)
        assertEquals(1, beanRepo.store.size)
        assertTrue(wishlistStore.isEmpty())
    }

    // AC-2 / AC-27: new object format with both purchases and wishlist
    @Test
    fun `new object format imports purchases and wishlist`() {
        val p = purchase(name = "Bean")
        val w = WishlistItem(UUID.randomUUID(), "Wish Bean", "R", "K")
        val json = objectMapper.writeValueAsBytes(mapOf("purchases" to listOf(p), "wishlist" to listOf(w)))
        val result = service.import(json)
        assertTrue(result.success)
        assertEquals(1, result.purchases)
        assertEquals(1, result.wishlist)
        assertEquals(1, beanRepo.store.size)
        assertEquals(1, wishlistStore.size)
    }

    // AC-3 / AC-4: duplicate UUID replaces existing record
    @Test
    fun `duplicate UUID purchase replaces existing`() {
        val id = UUID.randomUUID()
        val original = purchase(id = id, name = "Original")
        beanRepo.store.add(original)

        val updated = purchase(id = id, name = "Updated")
        val json = objectMapper.writeValueAsBytes(listOf(updated))
        service.import(json)

        assertEquals(1, beanRepo.store.size)
        assertEquals("Updated", beanRepo.store.first().name)
    }

    // AC-8: invalid JSON returns failure, no records written
    @Test
    fun `invalid JSON returns failure with no changes`() {
        val result = service.import("not json at all".toByteArray())
        assertFalse(result.success)
        assertTrue(beanRepo.store.isEmpty())
    }

    // AC-8: empty bytes returns failure
    @Test
    fun `empty bytes returns failure`() {
        val result = service.import(ByteArray(0))
        assertFalse(result.success)
    }

    // AC-9: individual record missing required field skipped, others imported
    @Test
    fun `record missing required id is skipped, others imported`() {
        val valid = purchase(name = "Valid")
        // Invalid: missing "id" field — use raw JSON
        val json = """{"purchases":[{"name":"NoId","roaster":"R","origin":"E","pricePerUnit":"10.00","weightGrams":100,"purchaseDate":"2025-01-01","roastDate":"2024-12-28","roastLevel":"MEDIUM","process":"WASHED","roastProfile":"FILTER"},${objectMapper.writeValueAsString(valid)}]}"""
        val result = service.import(json.toByteArray())
        assertTrue(result.success)
        assertEquals(1, result.purchases, "Only valid record imported")
        assertEquals(1, result.skipped, "One record skipped (missing id)")
    }

    // Risk Hotspot 1: legacy record without roastProfile gets OMNI default
    @Test
    fun `purchase without roastProfile defaults to OMNI and is not skipped`() {
        val json = """[{"id":"${UUID.randomUUID()}","name":"Old Bean","roaster":"R","origin":"E","pricePerUnit":"15.00","weightGrams":250,"purchaseDate":"2025-01-01","roastDate":"2024-12-28","roastLevel":"MEDIUM","process":"WASHED"}]"""
        val result = service.import(json.toByteArray())
        assertTrue(result.success)
        assertEquals(1, result.purchases, "Legacy record without roastProfile must be imported with OMNI default")
        assertEquals(RoastProfile.OMNI, beanRepo.store.first().roastProfile)
    }

    // AC-22 / AC-25 / Round-trip: export then re-import restores data
    @Test
    fun `export then re-import restores all purchases and wishlist items`() {
        val p1 = purchase(name = "Bean 1")
        val p2 = purchase(name = "Bean 2")
        beanRepo.store.addAll(listOf(p1, p2))
        wishlistStore.add(WishlistItem(UUID.randomUUID(), "Wish", "R", "K"))

        val exported = exportService.generateJson()

        // Clear stores
        beanRepo.store.clear()
        wishlistStore.clear()

        val result = service.import(exported)
        assertTrue(result.success)
        assertEquals(2, result.purchases)
        assertEquals(1, result.wishlist)
        assertEquals(2, beanRepo.store.size)
        assertEquals(1, wishlistStore.size)
    }
}
