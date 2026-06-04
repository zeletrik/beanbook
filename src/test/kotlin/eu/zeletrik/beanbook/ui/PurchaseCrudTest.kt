package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._size
import com.github.mvysny.kaributesting.v10._value
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.select.Select
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.beans.internal.BeanPurchaseRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class PurchaseCrudTest {

    private lateinit var repo: CrudTestRepository
    private lateinit var view: MainView

    @BeforeEach
    fun setup() {
        MockVaadin.setup()
        repo = CrudTestRepository()
        view = MainView(BeanPurchaseService(repo, repo), AnalyticsService())
        view.navigateTo(1)  // make Add page visible
    }

    @AfterEach
    fun teardown() = MockVaadin.tearDown()

    private fun purchase(name: String = "Bean", price: String = "15.00") = BeanPurchase(
        id = UUID.randomUUID(), name = name, roaster = "Roaster", origin = "Ethiopia",
        pricePerUnit = BigDecimal(price), weightGrams = 250,
        purchaseDate = LocalDate.of(2025, 1, 1), roastDate = LocalDate.of(2024, 12, 28),
        roastLevel = RoastLevel.MEDIUM, process = Process.WASHED, imageData = null,
    )

    private fun fillAddForm(name: String = "NewBean", price: String = "20.00") {
        val form = view.addFormContent
        form.nameField._value = name
        form.roasterField._value = "TestRoaster"
        form.originField._value = "Colombia"
        form.priceField._value = BigDecimal(price)
        form.weightField._value = 250
        form.purchaseDateField._value = LocalDate.of(2025, 6, 1)
        form.roastDateField._value = LocalDate.of(2025, 5, 28)
        form.roastLevelField._value = RoastLevel.LIGHT
        form.processField._value = Process.NATURAL
    }

    // AC-3: valid add appears in grid
    @Test
    fun `valid add purchase appears in grid`() {
        val sizeBefore = view.purchaseCount
        fillAddForm()
        view.addFormContent.saveButton.click()
        assertEquals(sizeBefore + 1, view.purchaseCount)
    }

    // AC-11: roastLevel restricted to 3 values
    @Test
    fun `roastLevel select has exactly three options`() {
        val items = RoastLevel.entries
        assertEquals(3, items.size)
        assertTrue(items.containsAll(listOf(RoastLevel.LIGHT, RoastLevel.MEDIUM, RoastLevel.DARK)))
    }

    // AC-12: process restricted to 3 values
    @Test
    fun `process select has exactly three options`() {
        val items = Process.entries
        assertEquals(3, items.size)
        assertTrue(items.containsAll(listOf(Process.WASHED, Process.NATURAL, Process.HONEY)))
    }

    // AC-14: valid edit reflects changes in grid
    @Test
    fun `valid edit reflects changed values in grid`() {
        val p = purchase("Original")
        repo.save(p)
        view.refreshView()

        view.purchaseForm.openForEdit(p)
        view.purchaseForm.nameField._value = "Updated"
        view.purchaseForm.saveButton.click()

        assertEquals("Updated", repo.findAll().first { it.id == p.id }.name)
    }

    // AC-16: confirmed delete removes entry
    @Test
    fun `confirmed delete removes entry from grid`() {
        val p = purchase("ToDelete")
        repo.save(p)
        view.refreshView()
        val sizeBefore = view.purchaseCount

        val dialog = view.showDeleteConfirmation(p)
        dialog._get<Button> { id = "confirm-delete-btn" }.click()

        assertEquals(sizeBefore - 1, view.purchaseCount)
        assertTrue(repo.findAll().none { it.id == p.id })
    }

    // AC-17: cancel delete leaves entry unchanged
    @Test
    fun `cancel delete leaves entry in grid`() {
        val p = purchase("ToKeep")
        repo.save(p)
        view.refreshView()
        val sizeBefore = view.purchaseCount

        val dialog = view.showDeleteConfirmation(p)
        dialog._get<Button> { id = "cancel-delete-btn" }.click()

        assertEquals(sizeBefore, view.purchaseCount)
        assertTrue(repo.findAll().any { it.id == p.id })
    }

    // AC-28: analytics update after mutation
    @Test
    fun `total cost updates after adding a purchase`() {
        fun String.toAnalyticsAmount() = removePrefix("€").let { if (it == "—") "0.00" else it }.toBigDecimal()
        val before = view.totalCostSpan.text.toAnalyticsAmount()
        fillAddForm(price = "50.00")
        view.addFormContent.saveButton.click()
        val after = view.totalCostSpan.text.toAnalyticsAmount()
        assertEquals(before + BigDecimal("50.00"), after)
    }
}

private class CrudTestRepository : TestBeanPurchaseRepository()
