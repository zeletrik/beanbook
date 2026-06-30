package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.vaadin.flow.component.UI
import eu.zeletrik.beanbook.ai.AiExtractionService
import eu.zeletrik.beanbook.ai.BeanExtraction
import eu.zeletrik.beanbook.ai.internal.BeanExtractionRunner
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.RoastProfile
import java.time.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/** Verifies the "Auto-fill from photo" flow: visibility gating, fill-blanks-only, AI-marking, and graceful failure. */
class PhotoAutoFillTest {

    @BeforeEach fun setup() {
        MockVaadin.setup()
        RecordedNotifications.install()
    }

    @AfterEach fun teardown() {
        MockVaadin.tearDown()
        RecordedNotifications.reset()
    }

    // Stub the page fetcher too so extractFromUrl never touches the network in tests.
    private fun stubService(result: BeanExtraction?) = AiExtractionService(
        runner = BeanExtractionRunner { result },
        fetcher = { "<html><body>coffee from somewhere</body></html>" },
    )

    private fun formWith(service: AiExtractionService?): PurchaseFormContent {
        val form = PurchaseFormContent(onSave = { _, _ -> }, aiExtractionService = service)
        UI.getCurrent().add(form)
        form.openForCreate()
        return form
    }

    @Test
    fun `the action is hidden when the AI feature is off`() {
        val plain = PurchaseFormContent(onSave = { _, _ -> })
        assertNull(plain.autoFillButton, "No photo button without the service")
        assertNull(plain.autoFillFromLinkButton, "No link button without the service")
        assertNull(testMainView(testRepository()).addFormContent.autoFillButton, "MainView hides it too")
    }

    @Test
    fun `clicking auto-fill from link applies the extraction`() {
        val form = formWith(stubService(BeanExtraction(name = "Linked Bean", origin = "Peru")))
        form.linkField.value = "https://roaster.example/linked-bean"

        form.autoFillFromLinkButton!!.click()

        assertEquals("Linked Bean", form.nameField.value)
        assertEquals("Peru", form.originField.value)
        assertTrue(form.nameField.hasClassName("ai-suggested"))
        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> !isError && text.contains("link") },
            "A success toast should confirm the link auto-fill, got: ${RecordedNotifications.shown}",
        )
    }

    @Test
    fun `auto-fill from link with an empty link asks for one`() {
        val form = formWith(stubService(BeanExtraction(name = "X")))
        form.linkField.value = ""

        form.autoFillFromLinkButton!!.click()

        assertTrue(form.nameField.value.isBlank(), "Nothing should be filled without a link")
        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> isError && text.contains("link") },
            "An error toast should prompt for a link, got: ${RecordedNotifications.shown}",
        )
    }

    @Test
    fun `the photo section leads the form`() {
        val form = formWith(stubService(BeanExtraction(name = "X")))
        assertTrue(form.photoSection.isVisible, "Photo card leads the form so the upload + auto-fill are up front")
        assertEquals(0, form.indexOf(form.photoSection), "Photo should be the first element of the form")
    }

    @Test
    fun `the action is present but disabled until a photo is staged`() {
        val form = formWith(stubService(BeanExtraction(name = "X")))
        val button = form.autoFillButton
        assertNotNull(button)
        assertFalse(button!!.isEnabled, "Disabled until an image is uploaded")
    }

    @Test
    fun `applying an extraction fills only blank fields and marks them as AI-suggested`() {
        val form = formWith(stubService(BeanExtraction()))
        form.nameField.value = "My Own Name" // user already typed this

        form.applyExtraction(
            BeanExtraction(
                name = "Should Be Ignored",
                roaster = "Acme Roasters",
                origin = "Kenya",
                region = "Nyeri",
                roastLevel = RoastLevel.LIGHT,
                process = Process.WASHED,
                weightGrams = 250,
                price = 12.5,
                notes = "blueberry, floral",
                roastDate = "2024-03-15",
            ),
        )

        // User-entered field is preserved and not marked.
        assertEquals("My Own Name", form.nameField.value)
        assertFalse(form.nameField.hasClassName("ai-suggested"))

        // Blank fields are filled and marked.
        assertEquals("Acme Roasters", form.roasterField.value)
        assertTrue(form.roasterField.hasClassName("ai-suggested"))
        assertEquals("Kenya", form.originField.value)
        assertEquals("Nyeri", form.regionField.value)
        assertTrue(form.regionField.hasClassName("ai-suggested"))
        assertEquals(RoastLevel.LIGHT, form.roastLevelField.value)
        assertEquals(Process.WASHED, form.processField.value)
        assertEquals(250, form.weightField.value)
        assertEquals("12.5", form.priceField.value)
        assertEquals("blueberry, floral", form.notesField.value)
        assertEquals(LocalDate.of(2024, 3, 15), form.roastDateField.value)
        assertTrue(form.roastDateField.hasClassName("ai-suggested"))
    }

    @Test
    fun `a printed brew profile overrides the default OMNI and is marked`() {
        val form = formWith(stubService(BeanExtraction()))
        // The roast-profile Select is never blank (defaults to OMNI), so a printed badge overrides it.
        form.applyExtraction(BeanExtraction(roastProfile = RoastProfile.ESPRESSO))

        assertEquals(RoastProfile.ESPRESSO, form.roastProfileField.value)
        assertTrue(form.roastProfileField.hasClassName("ai-suggested"))
    }

    @Test
    fun `a brew profile the user already chose is not overwritten`() {
        val form = formWith(stubService(BeanExtraction()))
        form.roastProfileField.value = RoastProfile.FILTER // user's own choice before auto-fill

        form.applyExtraction(BeanExtraction(roastProfile = RoastProfile.ESPRESSO))

        assertEquals(RoastProfile.FILTER, form.roastProfileField.value, "User's profile must be preserved")
        assertFalse(form.roastProfileField.hasClassName("ai-suggested"))
    }

    @Test
    fun `clicking auto-fill runs the extraction and confirms`() {
        val form = formWith(stubService(BeanExtraction(name = "Ethiopia Yirgacheffe", roaster = "Acme")))
        form.pendingImageData = byteArrayOf(1, 2, 3)
        form.pendingImageMimeType = "image/jpeg"
        val button = form.autoFillButton!!
        button.isEnabled = true

        button.click()

        assertEquals("Ethiopia Yirgacheffe", form.nameField.value)
        assertTrue(form.nameField.hasClassName("ai-suggested"))
        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> !isError && text.contains("photo") },
            "A success toast should confirm the auto-fill, got: ${RecordedNotifications.shown}",
        )
    }

    @Test
    fun `a failed extraction leaves the form untouched and explains`() {
        val form = formWith(stubService(null))
        form.pendingImageData = byteArrayOf(1, 2, 3)
        form.pendingImageMimeType = "image/png"
        val button = form.autoFillButton!!
        button.isEnabled = true

        button.click()

        assertTrue(form.nameField.value.isBlank(), "Nothing should be filled on failure")
        assertTrue(
            RecordedNotifications.shown.any { (text, isError) -> isError && text.contains("manually") },
            "An error toast should ask the user to fill it in manually, got: ${RecordedNotifications.shown}",
        )
    }
}
