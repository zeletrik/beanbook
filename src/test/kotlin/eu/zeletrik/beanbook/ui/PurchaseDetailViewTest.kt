package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
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
import java.util.UUID

/**
 * Verifies the redesigned detail view: Key facts / Bag tracking / Brew method render as cards, and the
 * bag-state timeline orders milestones chronologically (roast and purchase order is not fixed).
 */
class PurchaseDetailViewTest {

    @BeforeEach fun setup() = MockVaadin.setup()
    @AfterEach fun teardown() = MockVaadin.tearDown()

    private fun detailView(): PurchaseDetailView =
        PurchaseDetailView(onBack = {}, onEdit = {}, onDelete = {}, onSave = {}).also { UI.getCurrent().add(it) }

    private fun purchase(
        roastDate: LocalDate = LocalDate.of(2025, 1, 1),
        purchaseDate: LocalDate = LocalDate.of(2025, 1, 5),
        openedDate: LocalDate? = null,
        finishedDate: LocalDate? = null,
        roastProfile: RoastProfile = RoastProfile.ESPRESSO,
    ) = BeanPurchase(
        id = UUID.randomUUID(),
        name = "Yirgacheffe",
        roaster = "Onyx",
        origin = "Ethiopia",
        price = BigDecimal("20.00"),
        weightGrams = 250,
        purchaseDate = purchaseDate,
        roastDate = roastDate,
        roastLevel = RoastLevel.MEDIUM,
        process = Process.WASHED,
        roastProfile = roastProfile,
        openedDate = openedDate,
        finishedDate = finishedDate,
    )

    /** The milestone labels of the timeline, in render order (label is the first Span of each node). */
    private fun timelineLabels(view: PurchaseDetailView): List<String> =
        view._get<Div> { id = "detail-timeline" }.children.toList()
            .map { node -> node._find<Span> {}.first().text }

    @Test
    fun `detail view renders the section cards`() {
        val view = detailView()
        view.show(purchase(roastProfile = RoastProfile.OMNI))

        val titles = view._find<H3>().map { it.text }
        assertTrue(titles.contains("Key facts"), "found: $titles")
        assertTrue(titles.contains("Bag tracking"), "found: $titles")
        assertTrue(titles.contains("Brew method"), "OMNI beans show the brew-method card; found: $titles")
        assertTrue(view._find<Div> { id = "detail-timeline" }.isNotEmpty(), "timeline must render")
    }

    @Test
    fun `brew method card is hidden for non-OMNI beans`() {
        val view = detailView()
        view.show(purchase(roastProfile = RoastProfile.ESPRESSO))
        assertFalse(view._find<H3>().any { it.text == "Brew method" }, "non-OMNI beans hide the brew-method card")
    }

    @Test
    fun `timeline lists Roasted before Purchased when roasted earlier`() {
        val view = detailView()
        view.show(purchase(roastDate = LocalDate.of(2025, 1, 1), purchaseDate = LocalDate.of(2025, 1, 5)))
        assertEquals(listOf("Roasted", "Purchased", "Opened", "Finished"), timelineLabels(view))
    }

    @Test
    fun `timeline lists Purchased before Roasted for a roast-on-demand bag`() {
        val view = detailView()
        // Bought first, roasted later (roast-on-demand) — dates are out of the usual order.
        view.show(purchase(roastDate = LocalDate.of(2025, 1, 5), purchaseDate = LocalDate.of(2025, 1, 1)))
        assertEquals(listOf("Purchased", "Roasted", "Opened", "Finished"), timelineLabels(view))
    }

    @Test
    fun `timeline orders every dated milestone chronologically when fully tracked`() {
        val view = detailView()
        view.show(purchase(
            roastDate = LocalDate.of(2025, 1, 5),
            purchaseDate = LocalDate.of(2025, 1, 1), // roast-on-demand
            openedDate = LocalDate.of(2025, 1, 10),
            finishedDate = LocalDate.of(2025, 1, 20),
        ))
        assertEquals(listOf("Purchased", "Roasted", "Opened", "Finished"), timelineLabels(view))
    }

    @Test
    fun `not-yet milestones show a pending sub-line`() {
        val view = detailView()
        view.show(purchase(openedDate = null, finishedDate = null))

        val subs = view._get<Div> { id = "detail-timeline" }._find<Span> {}.map { it.text }
        assertTrue(subs.contains("Not opened yet"), "pending Opened node shows its placeholder")
        assertTrue(subs.contains("Not finished"), "pending Finished node shows its placeholder")
    }
}
