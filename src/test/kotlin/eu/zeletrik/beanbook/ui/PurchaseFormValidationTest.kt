package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._size
import com.github.mvysny.kaributesting.v10._value
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.ExportService
import tools.jackson.module.kotlin.jacksonObjectMapper
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.beans.internal.BeanPurchaseRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class PurchaseFormValidationTest {

    private lateinit var repo: TestRepository
    private lateinit var view: MainView
    private lateinit var addForm: PurchaseFormContent

    @BeforeEach
    fun setup() {
        MockVaadin.setup()
        repo = TestRepository()
        view = MainView(BeanPurchaseService(repo, repo), AnalyticsService(), ExportService(BeanPurchaseService(repo, repo), object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())
        view.navigateTo(2)  // make Add page visible
        addForm = view.addFormContent
    }

    @AfterEach
    fun teardown() = MockVaadin.tearDown()

    private fun fillValidForm(form: PurchaseFormContent = addForm) {
        form.nameField._value = "Yirgacheffe"
        form.roasterField._value = "Square Mile"
        form.originField._value = "Ethiopia"
        form.priceField._value = BigDecimal("18.50")
        form.weightField._value = 250
        form.purchaseDateField._value = LocalDate.of(2025, 1, 1)
        form.roastDateField._value = LocalDate.of(2024, 12, 28)
        form.roastLevelField._value = RoastLevel.MEDIUM
        form.processField._value = Process.WASHED
    }

    // AC-4: missing required field rejects submission, no entry saved
    @Test
    fun `submit with blank name is rejected — no entry saved`() {
        fillValidForm()
        addForm.nameField._value = ""
        val countBefore = view.purchaseCount
        addForm.saveButton.click()
        assertEquals(countBefore, view.purchaseCount)
    }

    @Test
    fun `submit with blank roaster is rejected`() {
        fillValidForm()
        addForm.roasterField._value = ""
        val countBefore = view.purchaseCount
        addForm.saveButton.click()
        assertEquals(countBefore, view.purchaseCount)
    }

    @Test
    fun `submit with blank origin is rejected`() {
        fillValidForm()
        addForm.originField._value = ""
        val countBefore = view.purchaseCount
        addForm.saveButton.click()
        assertEquals(countBefore, view.purchaseCount)
    }

    // AC-5: pricePerUnit > 0 accepted
    @Test
    fun `pricePerUnit greater than zero is accepted`() {
        fillValidForm()
        addForm.priceField._value = BigDecimal("0.01")
        val countBefore = view.purchaseCount
        addForm.saveButton.click()
        assertEquals(countBefore + 1, view.purchaseCount)
    }

    // AC-6: pricePerUnit = 0 rejected (boundary)
    @Test
    fun `pricePerUnit equal to zero is rejected`() {
        fillValidForm()
        addForm.priceField._value = BigDecimal.ZERO
        val countBefore = view.purchaseCount
        addForm.saveButton.click()
        assertEquals(countBefore, view.purchaseCount)
    }

    // AC-7: pricePerUnit < 0 rejected (beyond boundary)
    @Test
    fun `pricePerUnit less than zero is rejected`() {
        fillValidForm()
        addForm.priceField._value = BigDecimal("-1.00")
        val countBefore = view.purchaseCount
        addForm.saveButton.click()
        assertEquals(countBefore, view.purchaseCount)
    }

    // AC-8: weightGrams > 0 accepted
    @Test
    fun `weightGrams greater than zero is accepted`() {
        fillValidForm()
        addForm.weightField._value = 1
        val countBefore = view.purchaseCount
        addForm.saveButton.click()
        assertEquals(countBefore + 1, view.purchaseCount)
    }

    // AC-9: weightGrams = 0 rejected (boundary)
    @Test
    fun `weightGrams equal to zero is rejected`() {
        fillValidForm()
        addForm.weightField._value = 0
        val countBefore = view.purchaseCount
        addForm.saveButton.click()
        assertEquals(countBefore, view.purchaseCount)
    }

    // AC-10: weightGrams < 0 rejected (beyond boundary)
    @Test
    fun `weightGrams less than zero is rejected`() {
        fillValidForm()
        addForm.weightField._value = -1
        val countBefore = view.purchaseCount
        addForm.saveButton.click()
        assertEquals(countBefore, view.purchaseCount)
    }

    // AC-13: blank notes accepted (optional field)
    @Test
    fun `blank notes field is accepted without error`() {
        fillValidForm()
        addForm.notesField._value = ""
        val countBefore = view.purchaseCount
        addForm.saveButton.click()
        assertEquals(countBefore + 1, view.purchaseCount)
    }

    // AC-15: invalid edit preserves original values (uses edit dialog)
    @Test
    fun `invalid edit preserves original grid row values`() {
        fillValidForm()
        addForm.saveButton.click()
        val original = repo.findAll().last()
        val originalName = original.name

        view.purchaseForm.openForEdit(original)
        view.purchaseForm.nameField._value = ""
        view.purchaseForm.saveButton.click()

        assertEquals(originalName, repo.findAll().first { it.id == original.id }.name)
    }
}

private class TestRepository : TestBeanPurchaseRepository()
