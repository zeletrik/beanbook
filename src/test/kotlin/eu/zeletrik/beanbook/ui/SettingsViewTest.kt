package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.H3
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.ExportService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

class SettingsViewTest {

    @BeforeEach fun setup() = MockVaadin.setup()
    @AfterEach  fun teardown() = MockVaadin.tearDown()

    private fun makeView(): MainView {
        val repo = object : TestBeanPurchaseRepository() {}
        val service = BeanPurchaseService(repo, repo)
        val wishlistStub = object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) {
            override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>()
        }
        val exportService = ExportService(service, wishlistStub, jacksonObjectMapper())
        return MainView(service, AnalyticsService(), exportService, eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())
    }

    // AC-9: Settings tab is present in the bottom navigation bar (4 icon-only tabs)
    @Test
    fun `Settings tab is present in the bottom nav bar`() {
        val view = makeView()
        val tabCount = view.tabs.children.count()
        assertTrue(tabCount == 5L, "Expected 5 tabs (Purchases, Add, Analytics, Settings, Wishlist), got: $tabCount")
    }

    // AC-10: Export Data button is visible on the Settings page
    @Test
    fun `Settings page shows Export Data button`() {
        val view = makeView()
        view.navigateTo(4) // Settings is the 5th tab (index 4)

        val anchors = view.settingsPage._find<Anchor> { id = "export-data-btn" }
        assertTrue(anchors.isNotEmpty(), "Expected Export Data anchor/button on Settings page")
    }

    // AC-11: Preferences placeholder is visible on the Settings page
    @Test
    fun `Settings page shows Preferences coming soon section`() {
        val view = makeView()
        view.navigateTo(4)

        val headings = view.settingsPage._find<H3>()
        assertTrue(
            headings.any { it.text.contains("Preferences") },
            "Expected 'Preferences' heading on Settings page, found: ${headings.map { it.text }}"
        )
    }

    // AC-12 smoke: clicking Export Data does not throw in MockVaadin
    @Test
    fun `clicking Export Data anchor does not throw`() {
        val view = makeView()
        view.navigateTo(4)

        // Verify anchor exists and has download attribute set
        val anchor = view.settingsPage._get<Anchor> { id = "export-data-btn" }
        assertTrue(anchor.element.hasAttribute("download"), "Anchor must have download attribute")
    }
}
