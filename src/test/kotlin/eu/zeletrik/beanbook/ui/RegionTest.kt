package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._value
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.RoastProfile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** #19 — second-level origin (region): display label + form round-trip. */
class RegionTest {

    private lateinit var repo: TestBeanPurchaseRepository
    private lateinit var view: MainView

    @BeforeEach fun setup() {
        MockVaadin.setup()
        repo = object : TestBeanPurchaseRepository() {}
        view = testMainView(repo)
    }

    @AfterEach fun teardown() = MockVaadin.tearDown()

    private fun bean(region: String? = null) = BeanPurchase(
        id = UUID.randomUUID(), name = "Bean", roaster = "Roaster", origin = "Colombia",
        price = BigDecimal("18.50"), weightGrams = 250,
        purchaseDate = LocalDate.of(2025, 3, 10), roastDate = LocalDate.of(2025, 3, 5),
        roastLevel = RoastLevel.LIGHT, process = Process.WASHED, roastProfile = RoastProfile.OMNI,
        region = region,
    )

    private fun fillRequired(form: PurchaseFormContent) {
        form.nameField._value = "Bean"
        form.roasterField._value = "Roaster"
        form.originField._value = "Colombia"
        form.roastLevelField._value = RoastLevel.LIGHT
        form.processField._value = Process.WASHED
        form.priceField._value = "18.50"
        form.weightField._value = 250
        form.purchaseDateField._value = LocalDate.of(2025, 3, 10)
        form.roastDateField._value = LocalDate.of(2025, 3, 5)
    }

    @Test
    fun `originLabel appends the region when present`() {
        assertEquals("Colombia, Huila", bean(region = "Huila").originLabel())
    }

    @Test
    fun `originLabel is just the origin when the region is absent or blank`() {
        assertEquals("Colombia", bean(region = null).originLabel())
        assertEquals("Colombia", bean(region = "   ").originLabel())
    }

    @Test
    fun `region entered in the form is saved`() {
        view.navigateTo(AppTab.ADD)
        val form = view.addFormContent
        fillRequired(form)
        form.regionField._value = "Huila"
        form.saveButton.click()

        assertEquals("Huila", repo.findAll().single().region)
    }

    @Test
    fun `a blank region is stored as null`() {
        view.navigateTo(AppTab.ADD)
        val form = view.addFormContent
        fillRequired(form) // region left blank
        form.saveButton.click()

        assertNull(repo.findAll().single().region)
    }
}
