package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.button.Button
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.ExportService
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.RoastProfile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class RoastProfileFormTest {

    @BeforeEach fun setup() = MockVaadin.setup()
    @AfterEach  fun teardown() = MockVaadin.tearDown()

    private fun purchase(
        name: String = "Test Bean",
        roastProfile: RoastProfile = RoastProfile.FILTER,
        usedAs: RoastProfile? = null,
    ) = BeanPurchase(
        id = UUID.randomUUID(), name = name, roaster = "R", origin = "Ethiopia",
        pricePerUnit = BigDecimal("15.00"), weightGrams = 250,
        purchaseDate = LocalDate.of(2025, 1, 1), roastDate = LocalDate.of(2024, 12, 28),
        roastLevel = RoastLevel.MEDIUM, process = Process.WASHED,
        roastProfile = roastProfile, usedAs = usedAs,
    )

    private fun makeView(items: List<BeanPurchase> = emptyList()): MainView {
        val repo = object : TestBeanPurchaseRepository() { init { store.addAll(items) } }
        val service = BeanPurchaseService(repo, repo)
        return MainView(service, AnalyticsService(), ExportService(service, object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())
    }

    private fun fillRequired(view: MainView, name: String = "Bean") {
        view.addFormContent.nameField.value = name
        view.addFormContent.roasterField.value = "Roaster"
        view.addFormContent.originField.value = "Ethiopia"
        view.addFormContent.priceField.value = BigDecimal("15.00")
        view.addFormContent.weightField.value = 250
        view.addFormContent.purchaseDateField.value = LocalDate.of(2025, 1, 1)
        view.addFormContent.roastDateField.value = LocalDate.of(2024, 12, 28)
        view.addFormContent.roastLevelField.value = RoastLevel.MEDIUM
        view.addFormContent.processField.value = Process.WASHED
    }

    // AC-5: (a) roastProfileField is present in the add form
    @Test
    fun `roastProfileField is present in the add form`() {
        val view = makeView()
        view.navigateTo(2)
        assertTrue(view.addFormContent.roastProfileField.isVisible, "roastProfileField must be visible")
    }

    // AC-6: required validation — form rejects missing value
    @Test
    fun `form does not save when roastProfileField has no value`() {
        val repo = object : TestBeanPurchaseRepository() {}
        val service = BeanPurchaseService(repo, repo)
        val view = MainView(service, AnalyticsService(), ExportService(service, object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())
        view.navigateTo(2)
        fillRequired(view)
        view.addFormContent.roastProfileField.value = null
        view.addFormContent.saveButton.click()
        assertTrue(repo.store.isEmpty(), "Store must remain empty when roastProfile is not selected")
    }

    // AC-7: (b) OMNI is pre-selected on the new-bean form
    @Test
    fun `OMNI is pre-selected when new bean form opens`() {
        val view = makeView()
        view.navigateTo(2)
        assertEquals(RoastProfile.OMNI, view.addFormContent.roastProfileField.value)
    }

    // AC-8: (c) roastProfile is pre-populated with the bean's current value on edit
    @Test
    fun `roastProfile is pre-populated on edit with beans current value`() {
        val p = purchase(roastProfile = RoastProfile.ESPRESSO)
        val view = makeView(listOf(p))
        view.purchaseForm.openForEdit(p)
        assertEquals(RoastProfile.ESPRESSO, view.purchaseForm.roastProfileField.value)
    }

    // AC-9/AC-10: (e) saving edit OMNI→ESPRESSO clears usedAs
    @Test
    fun `saving edit from OMNI to ESPRESSO clears usedAs`() {
        val repo = object : TestBeanPurchaseRepository() {}
        val service = BeanPurchaseService(repo, repo)
        val view = MainView(service, AnalyticsService(), ExportService(service, object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())

        val omniBean = purchase(roastProfile = RoastProfile.OMNI, usedAs = RoastProfile.FILTER)
        repo.save(omniBean)
        view.refreshView()

        view.purchaseForm.openForEdit(omniBean)
        view.purchaseForm.roastProfileField.value = RoastProfile.ESPRESSO
        view.purchaseForm.saveButton.click()

        val updated = repo.store.first { it.id == omniBean.id }
        assertEquals(RoastProfile.ESPRESSO, updated.roastProfile)
        assertNull(updated.usedAs, "usedAs must be null after changing from OMNI to ESPRESSO")
    }

    // AC-11: (f) saving edit from ESPRESSO to OMNI does NOT set usedAs
    @Test
    fun `saving edit from ESPRESSO to OMNI does not set usedAs`() {
        val repo = object : TestBeanPurchaseRepository() {}
        val service = BeanPurchaseService(repo, repo)
        val view = MainView(service, AnalyticsService(), ExportService(service, object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())

        val espressoBean = purchase(roastProfile = RoastProfile.ESPRESSO, usedAs = null)
        repo.save(espressoBean)
        view.refreshView()

        view.purchaseForm.openForEdit(espressoBean)
        view.purchaseForm.roastProfileField.value = RoastProfile.OMNI
        view.purchaseForm.saveButton.click()

        val updated = repo.store.first { it.id == espressoBean.id }
        assertEquals(RoastProfile.OMNI, updated.roastProfile)
        assertNull(updated.usedAs, "usedAs must remain null after changing to OMNI")
    }

    // AC-17/AC-18: (i) duplicating OMNI bean with usedAs=ESPRESSO produces new record with same profile+usedAs
    @Test
    fun `duplicating OMNI bean with usedAs ESPRESSO produces new record with same profile and usedAs`() {
        val repo = object : TestBeanPurchaseRepository() {}
        val service = BeanPurchaseService(repo, repo)
        val view = MainView(service, AnalyticsService(), ExportService(service, object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())

        val omniBean = purchase(name = "Source", roastProfile = RoastProfile.OMNI, usedAs = RoastProfile.ESPRESSO)
        repo.save(omniBean)
        view.refreshView()

        // Trigger duplication
        view.detailView.show(omniBean)
        view.detailView._get<Button> { text = "Duplicate" }.click()

        // Fill required fields for the duplicate (name at minimum to make it unique)
        view.addFormContent.nameField.value = "Source Copy"
        view.addFormContent.roasterField.value = "R"
        view.addFormContent.originField.value = "E"
        view.addFormContent.priceField.value = BigDecimal("15.00")
        view.addFormContent.weightField.value = 250
        view.addFormContent.purchaseDateField.value = LocalDate.of(2025, 1, 1)
        view.addFormContent.roastDateField.value = LocalDate.of(2024, 12, 28)
        view.addFormContent.roastLevelField.value = RoastLevel.MEDIUM
        view.addFormContent.processField.value = Process.WASHED
        view.addFormContent.saveButton.click()

        val duplicate = repo.store.first { it.id != omniBean.id }
        assertEquals(RoastProfile.OMNI, duplicate.roastProfile, "Duplicate must carry over roastProfile")
        assertEquals(RoastProfile.ESPRESSO, duplicate.usedAs, "Duplicate must carry over usedAs")
    }

    // AC-13/AC-14: (d) Used As selector visible for OMNI, absent for ESPRESSO/FILTER
    @Test
    fun `Used As selector is visible for OMNI beans in detail view`() {
        val omniBean = purchase(roastProfile = RoastProfile.OMNI)
        val view = makeView(listOf(omniBean))
        view.detailView.show(omniBean)
        val selects = view.detailView._find<com.vaadin.flow.component.select.Select<*>> { id = "used-as-select" }
        assertTrue(selects.isNotEmpty(), "Used As selector must be present for OMNI beans")
    }

    @Test
    fun `Used As selector is absent for ESPRESSO beans in detail view`() {
        val espressoBean = purchase(roastProfile = RoastProfile.ESPRESSO)
        val view = makeView(listOf(espressoBean))
        view.detailView.show(espressoBean)
        val selects = view.detailView._find<com.vaadin.flow.component.select.Select<*>> { id = "used-as-select" }
        assertTrue(selects.isEmpty(), "Used As selector must NOT be present for ESPRESSO beans")
    }

    // AC-15: (g) changing Used As to ESPRESSO persists the value
    @Test
    fun `changing Used As to ESPRESSO in detail view persists usedAs`() {
        val repo = object : TestBeanPurchaseRepository() {}
        val service = BeanPurchaseService(repo, repo)
        val view = MainView(service, AnalyticsService(), ExportService(service, object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())

        val omniBean = purchase(roastProfile = RoastProfile.OMNI, usedAs = null)
        repo.save(omniBean)
        view.refreshView()
        view.detailView.show(omniBean)

        @Suppress("UNCHECKED_CAST")
        val usedAsSelect = view.detailView._get<com.vaadin.flow.component.select.Select<*>> { id = "used-as-select" }
            as com.vaadin.flow.component.select.Select<RoastProfile?>
        usedAsSelect.value = RoastProfile.ESPRESSO

        val updated = repo.store.first { it.id == omniBean.id }
        assertEquals(RoastProfile.ESPRESSO, updated.usedAs, "usedAs must be ESPRESSO after selection")
    }

    // AC-16: (h) clearing Used As persists null
    @Test
    fun `clearing Used As in detail view persists null usedAs`() {
        val repo = object : TestBeanPurchaseRepository() {}
        val service = BeanPurchaseService(repo, repo)
        val view = MainView(service, AnalyticsService(), ExportService(service, object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())

        val omniBean = purchase(roastProfile = RoastProfile.OMNI, usedAs = RoastProfile.FILTER)
        repo.save(omniBean)
        view.refreshView()
        view.detailView.show(omniBean)

        @Suppress("UNCHECKED_CAST")
        val usedAsSelect = view.detailView._get<com.vaadin.flow.component.select.Select<*>> { id = "used-as-select" }
            as com.vaadin.flow.component.select.Select<RoastProfile?>
        usedAsSelect.value = null

        val updated = repo.store.first { it.id == omniBean.id }
        assertNull(updated.usedAs, "usedAs must be null after clearing")
    }
}
