package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.ExportService
import tools.jackson.module.kotlin.jacksonObjectMapper
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.beans.internal.BeanPurchaseRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class MainViewTest {

    @BeforeEach fun setup() = MockVaadin.setup()
    @AfterEach  fun teardown() = MockVaadin.tearDown()

    private fun purchase(name: String = "Bean") = BeanPurchase(
        id = UUID.randomUUID(), name = name, roaster = "Roaster", origin = "Ethiopia",
        pricePerUnit = BigDecimal("15.00"), weightGrams = 250,
        purchaseDate = LocalDate.of(2025, 1, 1), roastDate = LocalDate.of(2024, 12, 28),
        roastLevel = RoastLevel.MEDIUM, process = Process.WASHED, imageData = null,
        roastProfile = eu.zeletrik.beanbook.beans.RoastProfile.FILTER,
    )

    private fun makeView(items: List<BeanPurchase>): MainView {
        val repo = object : TestBeanPurchaseRepository() {
            init { store.addAll(items) }
        }
        return MainView(BeanPurchaseService(repo, repo), AnalyticsService(), ExportService(BeanPurchaseService(repo, repo), object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())
    }

    // AC-1: cards show all entries
    @Test
    fun `cards show all purchase entries when list is non-empty`() {
        val view = makeView((1..5).map { purchase("Bean $it") })
        assertEquals(5, view.purchaseCount)
        assertTrue(view.cardsLayout.isVisible)
    }

    // AC-2: empty state shown when no purchases
    @Test
    fun `empty state message shown when list is empty`() {
        val view = makeView(emptyList())
        assertTrue(view.emptyStateMessage.isVisible)
        assertFalse(view.cardsLayout.isVisible)
    }

    // AC-2 inverse: empty state hidden when purchases present
    @Test
    fun `empty state message hidden when list is non-empty`() {
        val view = makeView(listOf(purchase()))
        assertFalse(view.emptyStateMessage.isVisible)
    }
}
