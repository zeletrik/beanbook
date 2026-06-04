package eu.zeletrik.beanbook.beans

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.MethodMode
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.nio.file.Path
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BeanPurchaseRestartTest {

    @Autowired
    private lateinit var service: BeanPurchaseService

    companion object {
        @TempDir
        @JvmStatic
        lateinit var tempDir: Path

        private val KNOWN_ID: UUID = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000001")

        private val KNOWN_BEAN = BeanPurchase(
            id = KNOWN_ID,
            name = "Restart Survivor",
            roaster = "Test Roaster",
            origin = "Brazil",
            pricePerUnit = BigDecimal("21.00"),
            weightGrams = 250,
            purchaseDate = LocalDate.of(2025, 5, 1),
            roastDate = LocalDate.of(2025, 4, 25),
            roastLevel = RoastLevel.MEDIUM,
            process = Process.NATURAL,
        )

        @DynamicPropertySource
        @JvmStatic
        fun configureDatabase(registry: DynamicPropertyRegistry) {
            val dbPath = tempDir.resolve("restart-test.db")
            registry.add("spring.datasource.url") {
                "jdbc:sqlite:${dbPath}?journal_mode=WAL"
            }
        }
    }

    // AC-1 + AC-16: save a record then force context restart → data must survive
    @Test
    @Order(1)
    @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
    fun `data is saved to the configured file path`() {
        service.save(KNOWN_BEAN)
        val found = service.findAll().any { it.id == KNOWN_ID }
        assertTrue(found, "saved bean should be findable in first context")
    }

    // AC-1: second context (fresh after dirty) reads the same file and finds the record
    @Test
    @Order(2)
    fun `data survives context restart`() {
        val found = service.findAll().firstOrNull { it.id == KNOWN_ID }
        val bean = checkNotNull(found) { "bean should survive context restart" }
        assertEquals("Restart Survivor", bean.name)
        assertEquals(BigDecimal("21.00"), bean.pricePerUnit)
    }

    // AC-17: the configured URL path is actually used (read/write go to the same store)
    @Test
    @Order(3)
    fun `configured URL path is used for reads and writes`() {
        val before = service.findAll().size
        val newBean = KNOWN_BEAN.copy(id = UUID.randomUUID(), name = "Config Path Bean")
        service.save(newBean)
        assertEquals(before + 1, service.findAll().size)
    }
}
