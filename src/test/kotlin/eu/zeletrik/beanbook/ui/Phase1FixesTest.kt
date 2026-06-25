package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.RoastProfile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

/** Covers the Phase 1 fixes: #18 (price locale), #21 (roaster typeahead), #22 (banner on detail), #17 (photo open). */
class Phase1FixesTest {

    private lateinit var repo: TestBeanPurchaseRepository
    private lateinit var view: MainView

    @BeforeEach fun setup() {
        MockVaadin.setup()
        repo = object : TestBeanPurchaseRepository() {}
        view = testMainView(repo)
    }

    @AfterEach fun teardown() = MockVaadin.tearDown()

    private fun bean(roaster: String = "Square Mile", sealed: Boolean = true, image: ByteArray? = null) =
        BeanPurchase(
            id = UUID.randomUUID(),
            name = "Yirgacheffe", roaster = roaster, origin = "Ethiopia",
            price = BigDecimal("18.50"), weightGrams = 250,
            purchaseDate = LocalDate.of(2025, 3, 10), roastDate = LocalDate.of(2025, 3, 5),
            roastLevel = RoastLevel.LIGHT, process = Process.WASHED, roastProfile = RoastProfile.OMNI,
            openedDate = if (sealed) null else LocalDate.of(2025, 3, 12),
            imageData = image,
        )

    private fun firstCard(): HorizontalLayout =
        view.cardsLayout.children.toList().filterIsInstance<HorizontalLayout>().first()

    // #18
    @Test
    fun `price field uses a dot decimal separator regardless of device locale`() {
        assertEquals(Locale.ENGLISH, view.addFormContent.priceField.locale)
    }

    // #21
    @Test
    fun `roaster field is seeded with existing roasters and still accepts custom values`() {
        repo.save(bean(roaster = "Square Mile"))
        repo.save(bean(roaster = "Acme Roasters"))
        view.refreshView()
        view.navigateTo(AppTab.ADD) // triggers openForCreate → seeds the suggestions

        val combo = view.addFormContent.roasterField
        assertTrue(combo.isAllowCustomValue, "Must still accept a brand-new roaster")
        val items = combo.genericDataView.items.toList()
        assertTrue(
            items.containsAll(listOf("Acme Roasters", "Square Mile")),
            "Roaster suggestions should include existing roasters, got: $items",
        )
    }

    // #22
    @Test
    fun `low-stock banner is hidden on the details page`() {
        repo.save(bean(sealed = false)) // no sealed bags → the banner shows on the list
        view.refreshView()
        assertTrue(view.lowStockBanner.isVisible, "Banner shows on the list when no sealed bags remain")

        firstCard()._click() // open the detail view

        assertFalse(view.lowStockBanner.isVisible, "Banner must be hidden on the detail page (#22)")
    }

    // #17
    @Test
    fun `tapping the detail photo opens it full-size`() {
        repo.save(bean(image = ByteArray(64) { 7 }))
        view.refreshView()
        firstCard()._click()

        view.detailView._get<Image> { id = "detail-photo" }._click()

        val dialog = _find<Dialog> { id = "detail-photo-dialog" }
        assertTrue(dialog.isNotEmpty() && dialog.first().isOpened, "Tapping the photo should open it full-size")
    }
}
