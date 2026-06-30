package eu.zeletrik.beanbook.beans

import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Verifies that [BeanPurchaseService.save] normalizes the free-text identity fields (roaster, origin,
 * region): case-insensitive dedup that keeps the first-seen spelling, trim + whitespace-collapse, and
 * blank region → null. Covers every write path since create/edit/duplicate/import all funnel through save.
 */
class BeanPurchaseServiceTest {

    private val repo = object : TestBeanPurchaseRepository() {}
    private val service = BeanPurchaseService(repo)

    private fun purchase(
        id: UUID = UUID.randomUUID(),
        roaster: String = "Roaster",
        origin: String = "Ethiopia",
        region: String? = null,
    ) = BeanPurchase(
        id = id,
        name = "Bean",
        roaster = roaster,
        origin = origin,
        price = BigDecimal("18.00"),
        weightGrams = 250,
        purchaseDate = LocalDate.of(2025, 1, 1),
        roastDate = LocalDate.of(2024, 12, 28),
        roastLevel = RoastLevel.MEDIUM,
        process = Process.WASHED,
        roastProfile = RoastProfile.OMNI,
        region = region,
    )

    @Test
    fun `saving a case-variant roaster reuses the existing spelling`() {
        service.save(purchase(roaster = "Onyx Coffee"))
        val saved = service.save(purchase(roaster = "onyx coffee"))
        assertEquals("Onyx Coffee", saved.roaster, "case variant must snap to the existing spelling")
        assertEquals(setOf("Onyx Coffee"), service.allRoasters().toSet(), "one roaster, not two")
    }

    @Test
    fun `a brand-new roaster keeps its intentional casing`() {
        val saved = service.save(purchase(roaster = "DAK Coffee Roasters"))
        assertEquals("DAK Coffee Roasters", saved.roaster)
    }

    @Test
    fun `whitespace is trimmed and internal runs collapsed`() {
        val saved = service.save(purchase(roaster = "  nomad   coffee "))
        assertEquals("nomad coffee", saved.roaster)
    }

    @Test
    fun `origin is normalized like roaster`() {
        service.save(purchase(origin = "Ethiopia"))
        val saved = service.save(purchase(origin = "ETHIOPIA"))
        assertEquals("Ethiopia", saved.origin)
    }

    @Test
    fun `region snaps to an existing spelling and a blank region becomes null`() {
        service.save(purchase(region = "Huila"))
        assertEquals("Huila", service.save(purchase(region = "huila")).region)
        assertNull(service.save(purchase(region = "   ")).region, "blank region normalizes to null")
    }

    @Test
    fun `editing the only holder of a value can re-case it`() {
        val id = UUID.randomUUID()
        service.save(purchase(id = id, roaster = "Onyx"))
        // No OTHER row holds "Onyx", so re-saving the same record with new casing sticks.
        assertEquals("ONYX", service.save(purchase(id = id, roaster = "ONYX")).roaster)
    }

    @Test
    fun `editing one of several holders snaps back to the shared casing`() {
        service.save(purchase(roaster = "Onyx"))
        val id = UUID.randomUUID()
        service.save(purchase(id = id, roaster = "Onyx"))
        // Another row still holds "Onyx", so re-casing this one is pulled back to the shared spelling.
        assertEquals("Onyx", service.save(purchase(id = id, roaster = "onyx")).roaster)
    }

    @Test
    fun `allRoasters collapses pre-existing case duplicates`() {
        // Seed the repo directly (bypassing save's normalization) to simulate legacy mixed-case rows.
        repo.store.add(purchase(roaster = "Onyx"))
        repo.store.add(purchase(roaster = "onyx"))
        assertEquals(1, service.allRoasters().size, "the typeahead must list one roaster, not two")
    }
}
