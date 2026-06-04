package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BagState
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.beans.internal.BeanPurchaseRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class CameraAndLowStockTest {

    private lateinit var repo: LowStockTestRepository
    private lateinit var view: MainView

    @BeforeEach
    fun setup() {
        MockVaadin.setup()
        repo = LowStockTestRepository()
        view = MainView(BeanPurchaseService(repo, repo), AnalyticsService())
    }

    @AfterEach
    fun teardown() = MockVaadin.tearDown()

    private fun purchase(state: BagState, name: String = "Bean"): BeanPurchase {
        val opened = if (state != BagState.SEALED) LocalDate.of(2025, 2, 1) else null
        val finished = if (state == BagState.FINISHED) LocalDate.of(2025, 3, 1) else null
        return BeanPurchase(
            id = UUID.randomUUID(), name = name, roaster = "R", origin = "E",
            pricePerUnit = BigDecimal("10.00"), weightGrams = 250,
            purchaseDate = LocalDate.of(2025, 1, 1), roastDate = LocalDate.of(2024, 12, 28),
            roastLevel = RoastLevel.MEDIUM, process = Process.WASHED,
            openedDate = opened, finishedDate = finished,
        )
    }

    // AC-5: upload component does NOT set capture attribute so iOS shows the full picker
    // (Take Photo + Photo Library + Files) rather than forcing camera-only
    @Test
    fun `upload component has no capture attribute so iOS shows full file picker`() {
        view.navigateTo(1)
        val capture = view.addFormContent.uploadComponent.element.getAttribute("capture")
        assertTrue(capture == null || capture.isEmpty(),
            "capture attribute must be absent to allow gallery selection on iOS")
    }

    // AC-6: regression — existing image upload tests still pass (upload pipeline unchanged)
    // This is verified implicitly by the full suite continuing to pass.

    // AC-7: low-stock banner shown when all purchases are Open (zero Sealed)
    @Test
    fun `low-stock banner shown when no sealed bags remain and list is non-empty`() {
        repo.save(purchase(BagState.OPEN, "OpenBean"))
        view.refreshView()
        assertTrue(view.lowStockBanner.isVisible)
    }

    // AC-7: low-stock banner shown when all purchases are Finished
    @Test
    fun `low-stock banner shown when all purchases are finished`() {
        repo.save(purchase(BagState.FINISHED, "FinishedBean"))
        view.refreshView()
        assertTrue(view.lowStockBanner.isVisible)
    }

    // AC-8: low-stock banner removed when a Sealed bag is added
    @Test
    fun `low-stock banner removed when a sealed bag is added`() {
        repo.save(purchase(BagState.OPEN, "Open1"))
        view.refreshView()
        assertTrue(view.lowStockBanner.isVisible)

        repo.save(purchase(BagState.SEALED, "NewSealed"))
        view.refreshView()
        assertFalse(view.lowStockBanner.isVisible)
    }

    // AC-9: low-stock banner not shown when at least one Sealed bag exists
    @Test
    fun `low-stock banner not shown when at least one sealed bag exists`() {
        repo.save(purchase(BagState.OPEN, "Open1"))
        repo.save(purchase(BagState.SEALED, "Reserve"))
        view.refreshView()
        assertFalse(view.lowStockBanner.isVisible)
    }

    // AC-10: low-stock banner not shown when list is empty
    @Test
    fun `low-stock banner not shown when purchase list is empty`() {
        view.refreshView()
        assertFalse(view.lowStockBanner.isVisible)
    }
}

private class LowStockTestRepository : TestBeanPurchaseRepository()
