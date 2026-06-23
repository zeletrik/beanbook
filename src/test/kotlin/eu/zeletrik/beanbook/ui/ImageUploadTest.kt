package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._size
import com.github.mvysny.kaributesting.v10._upload
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class ImageUploadTest {

    private lateinit var repo: ImageTestRepository
    private lateinit var view: MainView
    private lateinit var addForm: PurchaseFormContent

    @BeforeEach
    fun setup() {
        MockVaadin.setup()
        repo = ImageTestRepository()
        view = MainView(BeanPurchaseService(repo, repo), AnalyticsService(), ExportService(BeanPurchaseService(repo, repo), object : eu.zeletrik.beanbook.wishlist.WishlistService(org.springframework.jdbc.core.JdbcTemplate()) { override fun findAll() = emptyList<eu.zeletrik.beanbook.wishlist.WishlistItem>() }, jacksonObjectMapper()), eu.zeletrik.beanbook.TestImportService(), eu.zeletrik.beanbook.TestPreferencesService(), eu.zeletrik.beanbook.TestWishlistService())
        view.navigateTo(2)  // make Add page visible
        addForm = view.addFormContent
    }

    @AfterEach
    fun teardown() = MockVaadin.tearDown()

    private fun purchase(name: String = "Bean", imageData: ByteArray? = null) = BeanPurchase(
        id = UUID.randomUUID(), name = name, roaster = "Roaster", origin = "Ethiopia",
        pricePerUnit = BigDecimal("15.00"), weightGrams = 250,
        purchaseDate = LocalDate.of(2025, 1, 1), roastDate = LocalDate.of(2024, 12, 28),
        roastLevel = RoastLevel.MEDIUM, process = Process.WASHED, imageData = imageData,
        roastProfile = eu.zeletrik.beanbook.beans.RoastProfile.FILTER,
    )

    private fun fillValidForm(form: PurchaseFormContent = addForm) {
        form.nameField._value = "Bean"
        form.roasterField._value = "Roaster"
        form.originField._value = "Ethiopia"
        form.priceField._value = BigDecimal("15.00")
        form.weightField._value = 250
        form.purchaseDateField._value = LocalDate.of(2025, 1, 1)
        form.roastDateField._value = LocalDate.of(2024, 12, 28)
        form.roastLevelField._value = RoastLevel.MEDIUM
        form.processField._value = Process.WASHED
    }

    // AC-33: Upload restricted to JPEG, PNG, WebP
    @Test
    fun `upload component accepts only JPEG PNG and WebP file types`() {
        val acceptedTypes = addForm.uploadComponent.acceptedFileTypes
        assertTrue(acceptedTypes.contains("image/jpeg"))
        assertTrue(acceptedTypes.contains("image/png"))
        assertTrue(acceptedTypes.contains("image/webp"))
        assertTrue(acceptedTypes.size == 3)
    }

    // AC-34: Invalid format rejected server-side
    @Test
    fun `uploading unsupported MIME type is rejected — pendingImageData remains null`() {
        addForm.uploadComponent._upload("document.pdf", "application/pdf", ByteArray(100))
        assertNull(addForm.pendingImageData)
    }

    // AC-35: Form accepted without image
    @Test
    fun `form submission without image upload saves entry with null imageData`() {
        fillValidForm()
        addForm.saveButton.click()
        assertNull(repo.findAll().first().imageData)
    }

    // AC-36: File within size limit (1 byte — within bounds)
    @Test
    fun `uploading file smaller than 5 MB is accepted`() {
        addForm.uploadComponent._upload("photo.jpg", "image/jpeg", ByteArray(1))
        assertNotNull(addForm.pendingImageData)
    }

    // AC-37: File exactly at size limit (5,242,880 bytes — boundary)
    @Test
    fun `uploading file exactly 5 MB is accepted`() {
        addForm.uploadComponent._upload("photo.jpg", "image/jpeg", ByteArray(5_242_880))
        assertNotNull(addForm.pendingImageData)
    }

    // AC-38: File exceeding size limit (5,242,881 bytes — beyond boundary)
    @Test
    fun `uploading file larger than 5 MB is rejected`() {
        addForm.uploadComponent._upload("photo.jpg", "image/jpeg", ByteArray(5_242_881))
        assertNull(addForm.pendingImageData)
    }

    // AC-31: Image saved with entry when valid image uploaded
    @Test
    fun `image is saved and retrievable when valid image uploaded`() {
        val imageBytes = ByteArray(500) { it.toByte() }
        fillValidForm()
        addForm.uploadComponent._upload("photo.jpg", "image/jpeg", imageBytes)
        addForm.saveButton.click()
        val saved = repo.findAll().first()
        assertNotNull(saved.imageData)
        assertTrue(saved.imageData!!.contentEquals(imageBytes))
    }

    // AC-32: No error when entry has no image
    @Test
    fun `entry without image renders without error`() {
        val p = purchase(imageData = null)
        repo.save(p)
        view.refreshView()
        assertTrue(view.purchaseCount == 1)
    }

    // AC-31: Edit dialog shows image when entry has imageData
    @Test
    fun `edit dialog shows image when entry has imageData`() {
        val p = purchase(imageData = ByteArray(100) { 42 })
        view.purchaseForm.openForEdit(p)
        assertTrue(view.purchaseForm.currentImageDisplay.isVisible)
    }

    // AC-32: Edit dialog shows no image when entry has no imageData
    @Test
    fun `edit dialog does not show image when entry has no imageData`() {
        val p = purchase(imageData = null)
        view.purchaseForm.openForEdit(p)
        assertFalse(view.purchaseForm.currentImageDisplay.isVisible)
    }
}

private class ImageTestRepository : TestBeanPurchaseRepository()
