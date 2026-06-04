package eu.zeletrik.beanbook.beans

import eu.zeletrik.beanbook.beans.internal.BeanPurchaseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(properties = ["spring.datasource.url=jdbc:sqlite::memory:?journal_mode=WAL"])
class BeanPurchaseRepositoryTest {

    @Autowired
    private lateinit var service: BeanPurchaseService

    @Autowired
    private lateinit var repository: BeanPurchaseRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun clearTable() {
        jdbcTemplate.execute("DELETE FROM bean_purchases")
    }

    private fun samplePurchase(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Bean",
        pricePerUnit: BigDecimal = BigDecimal("18.50"),
        purchaseDate: LocalDate = LocalDate.of(2025, 1, 15),
    ) = BeanPurchase(
        id = id,
        name = name,
        roaster = "Test Roaster",
        origin = "Ethiopia",
        pricePerUnit = pricePerUnit,
        weightGrams = 250,
        purchaseDate = purchaseDate,
        roastDate = LocalDate.of(2025, 1, 10),
        roastLevel = RoastLevel.LIGHT,
        process = Process.NATURAL,
    )

    // AC-3: save with new ID inserts a record
    @Test
    fun `save with new id inserts record`() {
        val p = samplePurchase()
        service.save(p)
        val all = service.findAll()
        assertEquals(1, all.size)
        assertEquals(p.id, all.first().id)
    }

    // AC-2: save with existing ID replaces the record
    @Test
    fun `save with existing id replaces all fields`() {
        val original = samplePurchase(name = "Original")
        service.save(original)
        val updated = original.copy(name = "Updated", pricePerUnit = BigDecimal("25.00"))
        service.save(updated)
        val all = service.findAll()
        assertEquals(1, all.size)
        assertEquals("Updated", all.first().name)
        assertEquals(BigDecimal("25.00"), all.first().pricePerUnit)
    }

    // AC-4: delete removes the record
    @Test
    fun `deleteById removes an existing record`() {
        val p = samplePurchase()
        service.save(p)
        service.delete(p.id)
        assertTrue(service.findAll().isEmpty())
    }

    // AC-5: deleteById for missing id does not throw
    @Test
    fun `deleteById for non-existent id completes silently`() {
        service.delete(UUID.randomUUID()) // must not throw
    }

    // AC-6: String and Int fields round-trip exactly
    @Test
    fun `string and integer fields round-trip with exact values`() {
        val p = samplePurchase(name = "My Unique Bean").copy(
            roaster = "Precise Roaster",
            origin = "Colombia",
            weightGrams = 340,
        )
        service.save(p)
        val loaded = service.findAll().first()
        assertEquals("My Unique Bean", loaded.name)
        assertEquals("Precise Roaster", loaded.roaster)
        assertEquals("Colombia", loaded.origin)
        assertEquals(340, loaded.weightGrams)
    }

    // AC-7: BigDecimal precision preserved (TEXT storage)
    @Test
    fun `BigDecimal price_per_unit round-trips without precision loss`() {
        service.save(samplePurchase(pricePerUnit = BigDecimal("18.50")))
        assertEquals(BigDecimal("18.50"), service.findAll().first().pricePerUnit)
    }

    @Test
    fun `BigDecimal with many decimal places round-trips exactly`() {
        service.save(samplePurchase(pricePerUnit = BigDecimal("99.999")))
        assertEquals(BigDecimal("99.999"), service.findAll().first().pricePerUnit)
    }

    // AC-8: LocalDate fields round-trip as the same date
    @Test
    fun `LocalDate fields round-trip as the same date`() {
        val date = LocalDate.of(2025, 6, 15)
        service.save(samplePurchase(purchaseDate = date))
        assertEquals(date, service.findAll().first().purchaseDate)
    }

    // AC-9: Enum fields round-trip as the same enum constant
    @Test
    fun `RoastLevel enum round-trips as the same constant`() {
        service.save(samplePurchase().copy(roastLevel = RoastLevel.DARK))
        assertEquals(RoastLevel.DARK, service.findAll().first().roastLevel)
    }

    @Test
    fun `Process enum round-trips as the same constant`() {
        service.save(samplePurchase().copy(process = Process.WASHED))
        assertEquals(Process.WASHED, service.findAll().first().process)
    }

    // AC-10: ByteArray image round-trips byte-for-byte
    @Test
    fun `image_data ByteArray round-trips without corruption`() {
        val bytes = ByteArray(256) { it.toByte() }
        service.save(samplePurchase().copy(imageData = bytes))
        val loaded = service.findAll().first()
        assertNotNull(loaded.imageData)
        assertTrue(loaded.imageData!!.contentEquals(bytes))
    }

    // AC-11: Nullable fields stored and retrieved as null
    @Test
    fun `nullable fields stored and retrieved as null`() {
        service.save(samplePurchase()) // notes, grindSettings, imageData, rating, openedDate, finishedDate all null
        val loaded = service.findAll().first()
        assertNull(loaded.notes)
        assertNull(loaded.grindSettings)
        assertNull(loaded.imageData)
        assertNull(loaded.rating)
        assertNull(loaded.openedDate)
        assertNull(loaded.finishedDate)
    }

    @Test
    fun `non-null optional fields round-trip correctly`() {
        val p = samplePurchase().copy(
            notes = "Blueberry notes",
            grindSettings = "4 clicks",
            rating = 5,
            openedDate = LocalDate.of(2025, 1, 20),
            finishedDate = LocalDate.of(2025, 2, 10),
        )
        service.save(p)
        val loaded = service.findAll().first()
        assertEquals("Blueberry notes", loaded.notes)
        assertEquals("4 clicks", loaded.grindSettings)
        assertEquals(5, loaded.rating)
        assertEquals(LocalDate.of(2025, 1, 20), loaded.openedDate)
        assertEquals(LocalDate.of(2025, 2, 10), loaded.finishedDate)
    }

    // AC-22: test isolation — each test starts clean (@BeforeEach DELETE)
    @Test
    fun `table is empty at start of each test`() {
        assertTrue(service.findAll().isEmpty())
    }
}
