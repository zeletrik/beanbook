package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.button.Button
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BagState
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.ExportService
import tools.jackson.module.kotlin.jacksonObjectMapper
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.internal.BeanPurchaseSavePort
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class NotificationTest {

    @BeforeEach
    fun setup() {
        MockVaadin.setup()
        NotificationHelper._shown.clear()
    }

    @AfterEach
    fun teardown() {
        MockVaadin.tearDown()
        NotificationHelper._shown.clear()
    }

    private fun purchase(name: String = "Test Bean", bagState: BagState = BagState.SEALED): BeanPurchase {
        val openedDate = if (bagState != BagState.SEALED) LocalDate.of(2025, 1, 15) else null
        val finishedDate = if (bagState == BagState.FINISHED) LocalDate.of(2025, 2, 15) else null
        return BeanPurchase(
            id = UUID.randomUUID(), name = name, roaster = "R", origin = "Ethiopia",
            pricePerUnit = BigDecimal("15.00"), weightGrams = 250,
            purchaseDate = LocalDate.of(2025, 1, 1), roastDate = LocalDate.of(2024, 12, 28),
            roastLevel = RoastLevel.MEDIUM, process = Process.WASHED,
            openedDate = openedDate, finishedDate = finishedDate,
            roastProfile = eu.zeletrik.beanbook.beans.RoastProfile.FILTER,
        )
    }

    private fun makeView(repo: TestBeanPurchaseRepository, port: BeanPurchaseSavePort = repo): MainView =
        MainView(BeanPurchaseService(repo, port), AnalyticsService(), ExportService(BeanPurchaseService(repo, port), object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())

    private fun fillAddForm(view: MainView, name: String = "New Bean") {
        view.addFormContent.nameField.value = name
        view.addFormContent.roasterField.value = "Roaster"
        view.addFormContent.originField.value = "Ethiopia"
        view.addFormContent.priceField.value = BigDecimal("18.00")
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
        view.navigateTo(2)
        fillAddForm(view)

        view.addFormContent.saveButton.click()

        assertTrue(
            NotificationHelper._shown.any { (text, isError) -> text.contains("Bean saved") && !isError },
            "Expected 'Bean saved' success notification, got: ${NotificationHelper._shown}"
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
            NotificationHelper._shown.any { (text, isError) -> text.contains("Bean saved") && !isError },
            "Expected 'Bean saved' success notification, got: ${NotificationHelper._shown}"
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
            NotificationHelper._shown.any { (text, isError) -> text.contains("Bean deleted") && !isError },
            "Expected 'Bean deleted' success notification, got: ${NotificationHelper._shown}"
        )
    }

    // AC-3: save exception shows error notification
    @Test
    fun `save exception shows error notification`() {
        val repo = object : TestBeanPurchaseRepository() {}
        val failPort = object : BeanPurchaseSavePort {
            override fun <S : BeanPurchase> save(entity: S): S = throw RuntimeException("DB error")
        }
        val view = makeView(repo, failPort)
        view.navigateTo(2)
        fillAddForm(view, "Fail Bean")

        view.addFormContent.saveButton.click()

        assertTrue(
            NotificationHelper._shown.any { (text, isError) -> text.contains("Failed to save") && isError },
            "Expected save-error notification, got: ${NotificationHelper._shown}"
        )
    }

    // AC-4: save exception does NOT navigate away (Add form stays visible)
    @Test
    fun `save exception keeps user on current page`() {
        val repo = object : TestBeanPurchaseRepository() {}
        val failPort = object : BeanPurchaseSavePort {
            override fun <S : BeanPurchase> save(entity: S): S = throw RuntimeException("DB error")
        }
        val view = makeView(repo, failPort)
        view.navigateTo(2)
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
            override fun deleteById(id: UUID) = throw RuntimeException("Delete failed")
        }
        val view = makeView(failRepo)

        view.showDeleteConfirmation(p)
        _get<Button> { id = "confirm-delete-btn" }.click()

        assertTrue(
            NotificationHelper._shown.any { (text, isError) -> text.contains("Failed to delete") && isError },
            "Expected delete-error notification, got: ${NotificationHelper._shown}"
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
            NotificationHelper._shown.isEmpty(),
            "State transition should not trigger any notification, got: ${NotificationHelper._shown}"
        )
    }
}
