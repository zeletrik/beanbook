package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.button.Button
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BagState
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.backup.ExportService
import tools.jackson.module.kotlin.jacksonObjectMapper
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** Verifies user-facing notifications for save, delete, undo, and state-transition flows in [MainView]. */
class NotificationTest {

    @BeforeEach
    fun setup() {
        MockVaadin.setup()
        RecordedNotifications.install()
    }

    @AfterEach
    fun teardown() {
        MockVaadin.tearDown()
        RecordedNotifications.reset()
    }

    private fun purchase(name: String = "Test Bean", bagState: BagState = BagState.SEALED): BeanPurchase {
        val openedDate = if (bagState != BagState.SEALED) LocalDate.of(2025, 1, 15) else null
        val finishedDate = if (bagState == BagState.FINISHED) LocalDate.of(2025, 2, 15) else null
        return BeanPurchase(
            id = UUID.randomUUID(), name = name, roaster = "R", origin = "Ethiopia",
            price = BigDecimal("15.00"), weightGrams = 250,
            purchaseDate = LocalDate.of(2025, 1, 1), roastDate = LocalDate.of(2024, 12, 28),
            roastLevel = RoastLevel.MEDIUM, process = Process.WASHED,
            openedDate = openedDate, finishedDate = finishedDate,
            roastProfile = eu.zeletrik.beanbook.beans.RoastProfile.FILTER,
        )
    }

    /** A repository whose save() always fails, for exercising the save-error path. */
    private fun savingFailsRepo() = object : TestBeanPurchaseRepository() {
        override fun <S : BeanPurchase> save(entity: S): S = throw IllegalStateException("DB error")
    }

    private fun makeView(repo: TestBeanPurchaseRepository): MainView {
        return testMainView(repo)
    }

    private fun fillAddForm(view: MainView, name: String = "New Bean") {
        view.addFormContent.nameField.value = name
        view.addFormContent.roasterField.value = "Roaster"
        view.addFormContent.originField.value = "Ethiopia"
        view.addFormContent.priceField.value = "18.00"
        view.addFormContent.weightField.value = 250
        view.addFormContent.purchaseDateField.value = LocalDate.of(2025, 1, 1)
        view.addFormContent.roastDateField.value = LocalDate.of(2024, 12, 28)
        view.addFormContent.roastLevelField.value = RoastLevel.LIGHT
        view.addFormContent.processField.value = Process.NATURAL
    }

    // AC-1: new bean save shows "Bean saved"
    @Test
    fun `successful add save shows Bean saved notification`() {
        val repo = object : TestBeanPurchaseRepository() {}
        val view = makeView(repo)
        view.navigateTo(AppTab.ADD)
        fillAddForm(view)

        view.addFormContent.saveButton.click()

        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> text.contains("Bean saved") && !isError },
            "Expected 'Bean saved' success notification, got: ${RecordedNotifications.shown}"
        )
    }

    // AC-1: edit save also shows "Bean saved"
    @Test
    fun `successful edit save shows Bean saved notification`() {
        val p = purchase("Edit Me")
        val repo = object : TestBeanPurchaseRepository() { init { store.add(p) } }
        val view = makeView(repo)

        view.purchaseForm.openForEdit(p)
        view.purchaseForm.nameField.value = "Edited"
        view.purchaseForm.saveButton.click()

        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> text.contains("Bean saved") && !isError },
            "Expected 'Bean saved' success notification, got: ${RecordedNotifications.shown}"
        )
    }

    // AC-2: confirmed delete shows "Bean deleted"
    @Test
    fun `successful delete shows Bean deleted notification`() {
        val p = purchase("To Delete")
        val repo = object : TestBeanPurchaseRepository() { init { store.add(p) } }
        val view = makeView(repo)

        view.showDeleteConfirmation(p)
        _get<Button> { id = "confirm-delete-btn" }.click()

        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> text.contains("Bean deleted") && !isError },
            "Expected 'Bean deleted' success notification, got: ${RecordedNotifications.shown}"
        )
    }

    // Undo: clicking "Undo" on the delete toast re-saves the deleted purchase
    @Test
    fun `undo after delete restores the purchase`() {
        val p = purchase("Undo Me")
        val repo = object : TestBeanPurchaseRepository() { init { store.add(p) } }
        val view = makeView(repo)

        view.showDeleteConfirmation(p)
        _get<Button> { id = "confirm-delete-btn" }.click()
        assertTrue(repo.store.none { it.id == p.id }, "Purchase should be deleted before undo")

        _get<Button> { text = "Undo" }.click()

        assertTrue(repo.store.any { it.id == p.id }, "Undo should restore the deleted purchase")
    }

    // AC-3: save exception shows error notification
    @Test
    fun `save exception shows error notification`() {
        val view = makeView(savingFailsRepo())
        view.navigateTo(AppTab.ADD)
        fillAddForm(view, "Fail Bean")

        view.addFormContent.saveButton.click()

        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> text.contains("Failed to save") && isError },
            "Expected save-error notification, got: ${RecordedNotifications.shown}"
        )
    }

    // AC-4: save exception does NOT navigate away (Add form stays visible)
    @Test
    fun `save exception keeps user on current page`() {
        val view = makeView(savingFailsRepo())
        view.navigateTo(AppTab.ADD)
        fillAddForm(view, "Fail Bean")

        view.addFormContent.saveButton.click()

        assertTrue(view.addFormContent.isVisible, "Add form should remain visible after save failure")
    }

    // AC-5: delete exception shows error notification
    @Test
    fun `delete exception shows error notification`() {
        val p = purchase("To Delete Fail")
        val failRepo = object : TestBeanPurchaseRepository() {
            init { store.add(p) }
            override fun deleteById(id: UUID) = throw IllegalStateException("Delete failed")
        }
        val view = makeView(failRepo)

        view.showDeleteConfirmation(p)
        _get<Button> { id = "confirm-delete-btn" }.click()

        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> text.contains("Failed to delete") && isError },
            "Expected delete-error notification, got: ${RecordedNotifications.shown}"
        )
    }

    // AC-8: state transition (mark as opened) produces NO notification
    @Test
    fun `state transition mark as opened produces no notification`() {
        val p = purchase("Sealed Bean", bagState = BagState.SEALED)
        val repo = object : TestBeanPurchaseRepository() { init { store.add(p) } }
        val view = makeView(repo)

        view.detailView.show(p)
        view.detailView._get<Button> { text = "Mark as Opened Today" }.click()

        assertTrue(
            RecordedNotifications.shown.isEmpty(),
            "State transition should not trigger any notification, got: ${RecordedNotifications.shown}"
        )
    }
}
