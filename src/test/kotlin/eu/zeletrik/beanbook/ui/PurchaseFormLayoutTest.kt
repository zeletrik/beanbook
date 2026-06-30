package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.details.Details
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Verifies the New Purchase form's card-based layout: every group is an always-open [sectionCard]
 * (no accordions), fields land in the expected card, and Save floats in a sticky action bar.
 */
class PurchaseFormLayoutTest {

    @BeforeEach fun setup() = MockVaadin.setup()
    @AfterEach fun teardown() = MockVaadin.tearDown()

    private fun createForm(onCancel: (() -> Unit)? = null): PurchaseFormContent {
        val form = PurchaseFormContent(onSave = { _, _ -> }, onCancel = onCancel)
        UI.getCurrent().add(form)
        form.openForCreate()
        return form
    }

    private val sectionIdsInOrder = listOf(
        "section-photo", "section-bean", "section-roast",
        "section-purchase", "section-tasting", "section-tracking",
    )

    @Test
    fun `the form is six always-open cards, not accordions`() {
        val form = createForm()
        assertTrue(form._find<Details>().isEmpty(), "the redesign drops the collapsible Details accordions")
        sectionIdsInOrder.forEach { id ->
            assertTrue(form._find<Div> { this.id = id }.isNotEmpty(), "missing card '$id'")
        }
    }

    @Test
    fun `cards appear in the intended order with Photo leading`() {
        val form = createForm()
        val indices = sectionIdsInOrder.map { id -> form.indexOf(form._get<Div> { this.id = id }) }
        assertEquals(0, indices.first(), "Photo card leads the form")
        assertEquals(indices.sorted(), indices, "cards render in the declared order")
    }

    @Test
    fun `each field lands in its card`() {
        val form = createForm()
        fun card(id: String) = form._get<Div> { this.id = id }

        // Bean: identity fields + link
        assertSame(form.nameField, card("section-bean")._get<TextField> { id = "field-name" })
        assertSame(form.originField, card("section-bean")._get<TextField> { id = "field-origin" })
        assertSame(form.linkField, card("section-bean")._get<TextField> { id = "field-url" })
        // Purchase: transaction fields
        assertSame(form.priceField, card("section-purchase")._get<TextField> { id = "field-price" })
        assertSame(form.purchaseDateField, card("section-purchase")._get<DatePicker> { id = "field-purchase-date" })
        // Roast: the roast date lives with the roast group, not with Purchase
        assertSame(form.roastDateField, card("section-roast")._get<DatePicker> { id = "field-roast-date" })
        // Tasting: grind/notes; Tracking: the two state dates
        assertSame(form.grindSettingsField, card("section-tasting")._get<TextField> { id = "field-grind" })
        assertSame(form.openedDateField, card("section-tracking")._get<DatePicker> { id = "field-opened-date" })
        assertSame(form.finishedDateField, card("section-tracking")._get<DatePicker> { id = "field-finished-date" })
    }

    @Test
    fun `optional fields are visible up front (no expansion needed)`() {
        val form = createForm()
        // Previously these lived in collapsed accordions; now they're reachable immediately.
        listOf(form.tagsField, form.notesField, form.grindSettingsField, form.ratingField,
            form.openedDateField, form.finishedDateField).forEach {
            assertTrue(it.isVisible, "${it.id.orElse("field")} should be visible without expanding a section")
        }
    }

    @Test
    fun `Save lives in a sticky floating action bar`() {
        val form = createForm()
        val bar = form._get<HorizontalLayout> { id = "form-actions" }
        assertEquals("sticky", bar.style["position"], "the action bar floats (sticky) at the bottom")
        assertSame(form.saveButton, bar._get<Button> { text = "Save" })
    }

    @Test
    fun `Cancel appears only when the form is hosted with an onCancel (edit dialog)`() {
        val withCancel = createForm(onCancel = {})
        assertTrue(
            withCancel._get<HorizontalLayout> { id = "form-actions" }._find<Button> { text = "Cancel" }.isNotEmpty(),
            "the edit dialog gets a Cancel button",
        )

        UI.getCurrent().removeAll()
        val noCancel = createForm()
        assertTrue(
            noCancel._get<HorizontalLayout> { id = "form-actions" }._find<Button> { text = "Cancel" }.isEmpty(),
            "the full-page Add form has no Cancel",
        )
    }
}
