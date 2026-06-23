package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.ExportService
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class EmptyStateTest {

    @BeforeEach fun setup() = MockVaadin.setup()
    @AfterEach  fun teardown() = MockVaadin.tearDown()

    private fun purchase(name: String = "Bean ${ UUID.randomUUID() }") = BeanPurchase(
        id = UUID.randomUUID(), name = name, roaster = "R", origin = "Ethiopia",
        pricePerUnit = BigDecimal("15.00"), weightGrams = 250,
        purchaseDate = LocalDate.of(2025, 1, 1), roastDate = LocalDate.of(2024, 12, 28),
        roastLevel = RoastLevel.MEDIUM, process = Process.WASHED,
        roastProfile = eu.zeletrik.beanbook.beans.RoastProfile.FILTER,
    )

    private fun makeView(items: List<BeanPurchase> = emptyList()): MainView {
        val repo = object : TestBeanPurchaseRepository() { init { store.addAll(items) } }
        val service = BeanPurchaseService(repo, repo)
        return MainView(service, AnalyticsService(), ExportService(service, object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())
    }

    // AC-24: first-use empty state shows icon, "No beans yet" heading, and CTA
    @Test
    fun `empty database shows first-use empty state with heading and CTA`() {
        val view = makeView()

        assertTrue(view.emptyStateMessage.isVisible, "emptyStateMessage must be visible when DB is empty")

        val spans = view.emptyStateMessage._find<Span>()
        assertTrue(
            spans.any { it.text.contains("No beans yet") },
            "Expected 'No beans yet' heading, found: ${spans.map { it.text }}"
        )
        assertTrue(
            spans.any { it.text == "☕" },
            "Expected coffee cup icon span"
        )
    }

    // AC-25: clicking CTA navigates to Add tab (index 1)
    @Test
    fun `Add your first bean CTA navigates to the Add tab`() {
        val view = makeView()

        val cta = view.emptyStateMessage._get<Button> { id = "empty-state-cta" }
        cta.click()

        // After click, Add page (index 1) should be visible
        assertTrue(view.addFormContent.isVisible, "Add form should be visible after CTA click")
    }

    // AC-26: no-results empty state shows correct message when records exist but nothing matches
    @Test
    fun `search with no matches shows no-results empty state`() {
        val view = makeView(listOf(purchase("Ethiopian Light")))

        // Type something that won't match
        view.searchField.value = "zzz-no-match-zzz"

        assertTrue(view.emptyStateMessage.isVisible, "emptyStateMessage must be visible for no-results")
        val spans = view.emptyStateMessage._find<Span>()
        assertTrue(
            spans.any { it.text.contains("No beans match your search or filters") },
            "Expected no-results message, found: ${spans.map { it.text }}"
        )
    }

    // AC-27: no-results state does NOT show the add CTA button
    @Test
    fun `no-results empty state does not show Add your first bean button`() {
        val view = makeView(listOf(purchase()))

        view.searchField.value = "zzz-no-match-zzz"

        val ctaButtons = view.emptyStateMessage._find<Button> { id = "empty-state-cta" }
        assertTrue(ctaButtons.isEmpty(), "CTA button must not appear in no-results empty state")
    }

    // AC-28: clearing search restores the full list and hides empty state
    @Test
    fun `clearing search restores full list and hides empty state`() {
        val view = makeView(listOf(purchase("Kenya AA")))

        view.searchField.value = "zzz-no-match-zzz"
        assertTrue(view.emptyStateMessage.isVisible, "Empty state should show during no-results")

        view.searchField.value = ""

        assertFalse(view.emptyStateMessage.isVisible, "Empty state should be hidden after clearing search")
        assertTrue(view.cardsLayout.isVisible, "Cards layout should be visible after clearing search")
    }
}
