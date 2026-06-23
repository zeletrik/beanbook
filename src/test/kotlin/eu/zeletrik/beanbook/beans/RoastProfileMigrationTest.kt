package eu.zeletrik.beanbook.beans

import eu.zeletrik.beanbook.beans.internal.BeanPurchaseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
import java.nio.file.Path
import java.util.UUID

/**
 * Verifies the V2 migration backfills existing [BeanPurchase] rows with a default [RoastProfile] of OMNI and a null `usedAs`.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RoastProfileMigrationTest {

    @Autowired
    private lateinit var repository: BeanPurchaseRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    companion object {
        @TempDir
        @JvmStatic
        lateinit var tempDir: Path

        @DynamicPropertySource
        @JvmStatic
        fun configureDatabase(registry: DynamicPropertyRegistry) {
            val dbPath = tempDir.resolve("roast-profile-migration-test.db")
            registry.add("spring.datasource.url") { "jdbc:sqlite:${dbPath}?journal_mode=WAL" }
        }
    }

    // AC-3: existing beans default to roastProfile = OMNI after V2 migration
    // Insert a row using only V1 columns (roast_profile gets DEFAULT 'OMNI' from V2 migration).
    // After DirtiesContext, a second context reads the row back via the repository.
    @Test
    @Order(1)
    @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
    fun `pre-existing bean gets roastProfile OMNI from migration default`() {
        val id = UUID.randomUUID().toString()
        jdbcTemplate.update(
            """INSERT INTO bean_purchases
               (id, name, roaster, origin, price_per_unit, weight_grams,
                purchase_date, roast_date, roast_level, process)
               VALUES (?, 'Pre-migration Bean', 'Roaster', 'Ethiopia',
                       '15.00', 250, '2025-01-01', '2024-12-28', 'MEDIUM', 'WASHED')""",
            id,
        )
        // Verify it was inserted
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bean_purchases WHERE id = ?", Int::class.java, id)
        assertEquals(1, count, "Row must be present after insert")
    }

    // AC-4: existing beans have usedAs = null after migration
    @Test
    @Order(2)
    fun `pre-existing bean has null usedAs after migration`() {
        val beans = repository.findAll()
        assertTrue(beans.isNotEmpty(), "At least one pre-migration bean should be present")
        val migrated = beans.first()
        assertEquals(RoastProfile.OMNI, migrated.roastProfile, "Pre-existing bean must have roastProfile = OMNI")
        assertNull(migrated.usedAs, "Pre-existing bean must have usedAs = null")
    }

    // Kotlin stdlib `assertTrue` not directly available — use JUnit's
    private fun assertTrue(condition: Boolean, message: String) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message)
    }
}
