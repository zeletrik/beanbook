package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.upload.Upload
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.TestImportService
import eu.zeletrik.beanbook.TestPreferencesService
import eu.zeletrik.beanbook.TestWishlistService
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.backup.ExportService
import eu.zeletrik.beanbook.backup.ImportResult
import eu.zeletrik.beanbook.backup.ImportService
import eu.zeletrik.beanbook.wishlist.WishlistItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID

/** Verifies the Settings import/currency features and the wishlist add, delete, link, and empty-state behaviour in [MainView]. */
class ImportCurrencyWishlistTest {

    @BeforeEach fun setup() {
        MockVaadin.setup()
        RecordedNotifications.install()
    }
    @AfterEach fun teardown() {
        MockVaadin.tearDown()
        RecordedNotifications.reset()
    }

    private val wishlistStore = TestWishlistService()

    private fun makeView(
        importResult: ImportResult = ImportResult(0, 0, 0),
        prefsService: TestPreferencesService = TestPreferencesService(),
    ): MainView {
        val repo = object : TestBeanPurchaseRepository() {}
        val importStub = object : ImportService(BeanPurchaseService(repo), wishlistStore, jacksonObjectMapper()) {
            override fun import(bytes: ByteArray) = importResult
        }
        return testMainView(repo, wishlist = wishlistStore, importService = importStub, prefs = prefsService)
    }

    // AC-1: (a) import upload component is present in SettingsView
    @Test
    fun `Settings page has import upload component`() {
        val view = makeView()
        view.navigateTo(AppTab.SETTINGS)
        val uploads = view.settingsPage._find<Upload> { id = "import-upload" }
        assertTrue(uploads.isNotEmpty(), "Import upload component must be present in Settings page")
    }

    // AC-7: (b) valid import triggers success notification with counts
    @Test
    fun `valid import triggers success notification with counts`() {
        val view = makeView()
        view.navigateTo(AppTab.SETTINGS)
        val settingsView = view.settingsPage._find<SettingsView>().first()

        RecordedNotifications.shown.clear()
        settingsView.onImportFinished(ImportResult(3, 1, 2))

        assertTrue(
            RecordedNotifications.shown.any { (text, isError) ->
                !isError && text.contains("3 beans") && text.contains("1 wishlist") && text.contains("2 skipped")
            },
            "Expected success notification with counts, got: ${RecordedNotifications.shown}"
        )
    }

    // AC-8: (c) invalid JSON triggers error notification
    @Test
    fun `invalid JSON import triggers error notification`() {
        val view = makeView()
        view.navigateTo(AppTab.SETTINGS)
        val settingsView = view.settingsPage._find<SettingsView>().first()

        RecordedNotifications.shown.clear()
        settingsView.onImportFinished(ImportResult.FAILURE)

        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> isError && text.contains("Import failed") },
            "Expected error notification, got: ${RecordedNotifications.shown}"
        )
    }

    // #2: a failed import surfaces the specific reason, not just a generic message
    @Test
    fun `import failure notification includes the reason`() {
        val view = makeView()
        view.navigateTo(AppTab.SETTINGS)
        val settingsView = view.settingsPage._find<SettingsView>().first()

        RecordedNotifications.shown.clear()
        settingsView.onImportFinished(ImportResult.failure("the file isn't valid JSON"))

        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> isError && text.contains("isn't valid JSON") },
            "Failure notification should include the reason, got: ${RecordedNotifications.shown}"
        )
    }

    // AC-12: (d) currency selector is present in SettingsView
    @Test
    fun `Settings page has currency selector`() {
        val view = makeView()
        view.navigateTo(AppTab.SETTINGS)
        val selects = view.settingsPage._find<Select<*>> { id = "currency-select" }
        assertTrue(selects.isNotEmpty(), "Currency selector must be present")
    }

    // AC-18: currency change calls PreferencesService.setCurrency()
    @Test
    fun `changing currency selector calls setCurrency on PreferencesService`() {
        val prefs = TestPreferencesService()
        val view = makeView(prefsService = prefs)
        view.navigateTo(AppTab.SETTINGS)

        @Suppress("UNCHECKED_CAST")
        val currencySelect = view.settingsPage._get<Select<*>> { id = "currency-select" }
            as Select<String>
        currencySelect.value = "$"

        assertEquals("$", prefs.getCurrency(), "setCurrency should have been called with '$'")
    }

    // AC-21: (e) adding wishlist item with name saves it
    @Test
    fun `adding wishlist item with name saves and appears in list`() {
        val view = makeView()
        view.navigateTo(AppTab.WISHLIST) // Wishlist is the 4th tab (index 3)

        val wishlistView = view._find<WishlistView>().first()
        wishlistView._get<com.vaadin.flow.component.textfield.TextField> { id = "wishlist-name" }.value = "New Wish Bean"
        wishlistView._get<Button> { id = "wishlist-add-btn" }.click()

        assertEquals(1, wishlistStore.store.size, "Wishlist item must be saved")
        assertEquals("New Wish Bean", wishlistStore.store.first().name)
    }

    // AC-22: (f) adding without name does not save
    @Test
    fun `adding wishlist item without name does not save`() {
        val view = makeView()
        view.navigateTo(AppTab.WISHLIST)

        val wishlistView = view._find<WishlistView>().first()
        wishlistView._get<Button> { id = "wishlist-add-btn" }.click()

        assertTrue(wishlistStore.store.isEmpty(), "Empty name must not save a wishlist item")
        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> isError && text.contains("required") },
            "Error notification must be shown for empty name"
        )
    }

    // AC-23: (g) delete wishlist item removes it
    @Test
    fun `deleting a wishlist item removes it`() {
        val item = WishlistItem(UUID.randomUUID(), "To Delete", "R", "K")
        wishlistStore.store.add(item)

        val view = makeView()
        view.navigateTo(AppTab.WISHLIST)

        val wishlistView = view._find<WishlistView>().first()
        wishlistView.refreshList()

        wishlistView._get<Button> { id = "wishlist-delete-${item.id}" }.click()
        _get<Button> { id = "wishlist-confirm-delete-btn" }.click()

        assertTrue(wishlistStore.store.isEmpty(), "Item must be removed after confirming delete")
    }

    // AC-24: (h) empty wishlist shows empty state
    @Test
    fun `empty wishlist shows empty state`() {
        val view = makeView()
        view.navigateTo(AppTab.WISHLIST)

        val wishlistView = view._find<WishlistView>().first()
        val emptyState = wishlistView._find<com.vaadin.flow.component.html.Div> { id = "wishlist-empty-state" }
        assertTrue(emptyState.isNotEmpty() && emptyState.first().isVisible,
            "Empty state must be visible when wishlist is empty")
    }

    // #3: the add form is a collapsible section, collapsed by default so it doesn't block the list
    @Test
    fun `wishlist add form is collapsed by default`() {
        val view = makeView()
        view.navigateTo(AppTab.WISHLIST)

        val wishlistView = view._find<WishlistView>().first()
        val addSection = wishlistView._get<com.vaadin.flow.component.details.Details> { id = "wishlist-add-section" }
        assertFalse(addSection.isOpened, "Add form should start collapsed")
    }

    // #3: adding a wishlist item with a link normalises and stores the url
    @Test
    fun `adding wishlist item with link stores normalised url`() {
        val view = makeView()
        view.navigateTo(AppTab.WISHLIST)

        val wishlistView = view._find<WishlistView>().first()
        wishlistView._get<com.vaadin.flow.component.textfield.TextField> { id = "wishlist-name" }.value = "Linked Wish"
        wishlistView._get<com.vaadin.flow.component.textfield.TextField> { id = "wishlist-url" }.value = "roaster.com/wish"
        wishlistView._get<Button> { id = "wishlist-add-btn" }.click()

        assertEquals("https://roaster.com/wish", wishlistStore.store.first { it.name == "Linked Wish" }.url)
    }

    // #3: a wishlist item is openable into a detail dialog exposing its clickable link
    @Test
    fun `opening a wishlist item shows detail with clickable link`() {
        val item = WishlistItem(UUID.randomUUID(), "Openable", "Roaster", "Kenya", "nice", "https://roaster.com/openable")
        wishlistStore.store.add(item)

        val view = makeView()
        view.navigateTo(AppTab.WISHLIST)
        val wishlistView = view._find<WishlistView>().first()
        wishlistView.refreshList()

        wishlistView.openDetail(item)

        val link = _get<com.vaadin.flow.component.html.Anchor> { id = "wishlist-detail-link" }
        assertEquals("https://roaster.com/openable", link.href)
    }
}
