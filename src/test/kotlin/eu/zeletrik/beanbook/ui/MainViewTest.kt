package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.button.Button
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.backup.ExportService
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

/** Tests for [MainView]: purchase card rendering, empty states, search/filter, and the analytics panel. */
class MainViewTest {

    @BeforeEach fun setup() = MockVaadin.setup()
    @AfterEach  fun teardown() = MockVaadin.tearDown()

    private fun purchase(name: String = "Bean") = BeanPurchase(
        id = UUID.randomUUID(), name = name, roaster = "Roaster", origin = "Ethiopia",
        price = BigDecimal("15.00"), weightGrams = 250,
        purchaseDate = LocalDate.of(2025, 1, 1), roastDate = LocalDate.of(2024, 12, 28),
        roastLevel = RoastLevel.MEDIUM, process = Process.WASHED, imageData = null,
        roastProfile = eu.zeletrik.beanbook.beans.RoastProfile.FILTER,
    )

    private fun makeView(items: List<BeanPurchase>): MainView {
        return testMainView(testRepository(items))
    }

    // §U5.8: applying the filter dialog must not wipe the active search query.
    @Test
    fun `applying the filter dialog preserves the active search query`() {
        val view = makeView(listOf(purchase("Ethiopia Natural")))
        view.searchField.value = "ethiop"
        assertEquals("ethiop", view.filterState.query)

        view.filterSortDialog.openWith(view.filterState)
        view.filterSortDialog._get<Button> { text = "Apply" }.click()

        assertEquals("ethiop", view.filterState.query, "Search query must survive applying the filter dialog")
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

    // Analytics panel shows its own empty state ("Add beans to see your stats") with no purchases
    @Test
    fun `analytics panel empty state visible when no purchases`() {
        val view = makeView(emptyList())
        view.navigateTo(AppTab.ANALYTICS)

        val emptyState = view._find<com.vaadin.flow.component.html.Div> { id = "analytics-empty-state" }
        assertTrue(
            emptyState.isNotEmpty() && emptyState.first().isVisible,
            "Analytics empty state must be visible when there are no purchases",
        )
    }

    // Inverse: real stats render (empty state hidden) when purchases exist
    @Test
    fun `analytics panel empty state hidden when purchases present`() {
        val view = makeView(listOf(purchase()))
        view.navigateTo(AppTab.ANALYTICS)

        // _find returns only visible components, so an invisible empty state yields an empty list.
        val emptyState = view._find<com.vaadin.flow.component.html.Div> { id = "analytics-empty-state" }
        assertTrue(
            emptyState.isEmpty(),
            "Analytics empty state must be hidden when purchases exist",
        )
    }
}
