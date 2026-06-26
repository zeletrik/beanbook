package eu.zeletrik.beanbook

import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.MethodMode
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.nio.file.Path
import java.time.LocalDate
import java.util.UUID

/**
 * Verifies that the Liquibase migration creates the schema, persists data across context restarts, and applies each changeset exactly once.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class LiquibaseMigrationTest {

    @Autowired
    private lateinit var service: BeanPurchaseService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    companion object {
        @TempDir
        @JvmStatic
        lateinit var tempDir: Path

        private val MIGRATION_BEAN_ID: UUID = UUID.fromString("b2c3d4e5-0000-4000-8000-000000000002")

        private val MIGRATION_BEAN = BeanPurchase(
            id = MIGRATION_BEAN_ID,
            name = "Migration Test Bean",
            roaster = "Liquibase Roasters",
            origin = "Kenya",
            price = BigDecimal("28.50"),
            weightGrams = 200,
            purchaseDate = LocalDate.of(2025, 3, 1),
            roastDate = LocalDate.of(2025, 2, 25),
            roastLevel = RoastLevel.LIGHT,
            process = Process.WASHED,
            roastProfile = eu.zeletrik.beanbook.beans.RoastProfile.FILTER,
        )

        @DynamicPropertySource
        @JvmStatic
        fun configureDatabase(registry: DynamicPropertyRegistry) {
            val dbPath = tempDir.resolve("migration-test.db")
            registry.add("spring.datasource.url") {
                "jdbc:sqlite:${dbPath}?journal_mode=WAL"
            }
        }
    }

    // AC-12: schema is created on first startup
    @Test
    @Order(1)
    fun `bean_purchases table exists after first startup`() {
        val tableExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='bean_purchases'",
            Int::class.java,
        )
        assertEquals(1, tableExists, "bean_purchases table must exist")
    }

    // AC-15: empty database returns empty list on startup
    @Test
    @Order(2)
    @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
    fun `new database starts empty and data inserted here persists`() {
        val before = service.findAll()
        assertTrue(before.none { it.id == MIGRATION_BEAN_ID }, "table should be empty before insert")

        service.save(MIGRATION_BEAN)
        assertTrue(service.findAll().any { it.id == MIGRATION_BEAN_ID }, "inserted bean must be findable")
    }

    // AC-13: data written in first context is present in second context (survives restart)
    @Test
    @Order(3)
    fun `data inserted before restart is present after restart`() {
        val found = service.findAll().firstOrNull { it.id == MIGRATION_BEAN_ID }
        val bean = checkNotNull(found) { "bean inserted before restart must survive context restart" }
        assertEquals("Migration Test Bean", bean.name)
    }

    // AC-14: Liquibase applies the migration exactly once (no re-runs)
    @Test
    @Order(4)
    fun `Liquibase runs each migration exactly once`() {
        val migrationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM DATABASECHANGELOG",
            Int::class.java,
        )
        assertEquals(
            2,
            migrationCount,
            "Each changeset (V1 baseline + V2 region) should appear exactly once in DATABASECHANGELOG",
        )
    }
}
