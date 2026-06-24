package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasStyle
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.details.Details
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.BigDecimalField
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.component.upload.receivers.MemoryBuffer
import com.vaadin.flow.data.binder.Binder
import eu.zeletrik.beanbook.ai.AiExtractionService
import eu.zeletrik.beanbook.ai.BeanExtraction
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.RoastProfile
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

private const val MAX_IMAGE_BYTES = 5_242_880
private val ALLOWED_TYPES = setOf("image/jpeg", "image/png", "image/webp")


private const val MAX_TAG_LENGTH = 20
private const val MAX_TAG_COUNT = 10
private const val REQUIRED = "Required"
private const val AI_CLASS = "ai-suggested"
private const val DEFAULT_IMAGE_MIME = "image/jpeg"

/**
 * Reusable form body for creating, editing, or re-purchasing a [BeanPurchase].
 *
 * Holds all input fields, drives validation through a [Binder], and exposes
 * [openForCreate], [openForEdit], and [openWithProfile] to populate the fields.
 */
class PurchaseFormContent(
    private val onSave: (bean: PurchaseFormBean, existingId: UUID?) -> Unit,
    private val onCancel: (() -> Unit)? = null,
    private val getAllTags: () -> Set<String> = { emptySet() },
    /** When present, enables "Auto-fill from photo"; null hides the action entirely (feature off). */
    private val aiExtractionService: AiExtractionService? = null,
) : VerticalLayout() {

    internal val nameField = TextField("Name").also { it.setId("field-name"); it.isRequiredIndicatorVisible = true }
    internal val roasterField = TextField("Roaster").also { it.setId("field-roaster"); it.isRequiredIndicatorVisible = true }
    internal val originField = TextField("Origin").also { it.setId("field-origin"); it.isRequiredIndicatorVisible = true }
    internal val priceField = BigDecimalField("Price per unit").also { it.setId("field-price"); it.isRequiredIndicatorVisible = true }
    internal val weightField = IntegerField("Weight (g)").also { it.setId("field-weight"); it.isRequiredIndicatorVisible = true }
    internal val purchaseDateField = DatePicker("Purchase date").also { it.setId("field-purchase-date") }
    internal val roastDateField = DatePicker("Roast date").also { it.setId("field-roast-date") }
    internal val roastLevelField = Select<RoastLevel>().also {
        it.setId("field-roast-level")
        it.label = "Roast level"
        it.setItems(*RoastLevel.entries.toTypedArray())
    }
    internal val processField = Select<Process>().also {
        it.setId("field-process")
        it.label = "Process"
        it.setItems(*Process.entries.toTypedArray())
    }
    internal val roastProfileField = Select<RoastProfile>().also {
        it.setId("field-roast-profile")
        it.label = "Roast profile"
        it.setItems(*RoastProfile.entries.toTypedArray())
        it.setItemLabelGenerator { rp -> rp.displayName() }
    }
    internal val notesField = TextArea("Notes").also { it.setId("field-notes") }
    internal val tagsField = MultiSelectComboBox<String>("Tags").also {
        it.setId("field-tags")
        it.setAllowCustomValue(true)
    }
    internal val grindSettingsField = TextField("Grind settings").also { it.setId("field-grind") }

    /** Optional link to the bean's product page or the roaster's profile. */
    internal val linkField = TextField("Link").also {
        it.setId("field-url")
        it.width = "100%"
        it.placeholder = "Product or roaster page"
        it.setClearButtonVisible(true)
    }

    /** Rating selector ranging from 1 to 5; a null value means the bean is not rated. */
    internal val ratingField = Select<Int>().also {
        it.setId("field-rating")
        it.label = "Rating"
        it.setPlaceholder("Not rated")
        it.setItems(1, 2, 3, 4, 5)
        it.setItemLabelGenerator { r -> r.toStars() }
    }

    // State tracker dates (optional)
    internal val openedDateField = DatePicker("Opened").also {
        it.setId("field-opened-date")
        it.isClearButtonVisible = true
    }
    internal val finishedDateField = DatePicker("Finished").also {
        it.setId("field-finished-date")
        it.isClearButtonVisible = true
    }

    // Image upload
    internal var pendingImageData: ByteArray? = null
    internal var pendingImageMimeType: String? = null

    /**
     * "Auto-fill from photo" action; created only when [aiExtractionService] is present so the UI hides
     * cleanly when the AI feature is off. Enabled once a valid image is staged.
     */
    internal val autoFillButton: Button? = aiExtractionService?.let {
        Button("Auto-fill from photo", Icon(VaadinIcon.MAGIC)) { autoFillFromPhoto() }.apply {
            setId("ai-autofill-btn")
            isEnabled = false
            addThemeVariants(ButtonVariant.LUMO_TERTIARY)
        }
    }

    /** "Auto-fill from link" action; created only when the AI feature is on. Reads the Link field's URL. */
    internal val autoFillFromLinkButton: Button? = aiExtractionService?.let {
        Button("Auto-fill from link", Icon(VaadinIcon.MAGIC)) { autoFillFromLink() }.apply {
            setId("ai-autofill-link-btn")
            addThemeVariants(ButtonVariant.LUMO_TERTIARY)
        }
    }
    private val uploadHint = Span("JPEG, PNG, or WebP · max 5 MB").also {
        it.style["font-size"] = "var(--lumo-font-size-xs)"
        it.style["color"] = "var(--lumo-secondary-text-color)"
    }
    private val uploadErrorLabel = Span().also {
        it.setId("upload-error")
        it.isVisible = false
        it.style["color"] = "var(--lumo-error-color)"
        it.style["font-size"] = "var(--lumo-font-size-s)"
        // Constrain so a long message wraps instead of overflowing the collapsed section on mobile.
        it.style["max-width"] = "100%"
        it.style["word-break"] = "break-word"
    }
    internal val currentImageDisplay = Image().also {
        it.setId("current-image")
        it.isVisible = false
        it.setHeight("80px")
    }
    private val existingImageLabel = Span("Image already uploaded").also { it.isVisible = false }

    private val buffer = MemoryBuffer()
    internal val uploadComponent = Upload(buffer).also {
        it.setId("image-upload")
        it.setAcceptedFileTypes(*ALLOWED_TYPES.toTypedArray())
        it.setMaxFileSize(MAX_IMAGE_BYTES)
    }

    private val binder = Binder<PurchaseFormBean>()
    private var editingId: UUID? = null

    internal val saveButton = Button("Save") {
        if (binder.validate().isOk) {
            val bean = binder.bean
            bean.imageData = pendingImageData ?: bean.imageData
            onSave(bean, editingId)
        }
    }.apply { addThemeVariants(ButtonVariant.LUMO_PRIMARY) }

    // ── Optional, collapsible sections (collapsed on create; opened on edit when they hold data) ──
    internal val tastingDetails = Details(
        "Tasting notes & tags",
        FormLayout(ratingField, tagsField, grindSettingsField, notesField),
    ).apply { setId("section-tasting") }

    internal val trackingDetails = Details(
        "Bag tracking",
        FormLayout(openedDateField, finishedDateField),
    ).apply { setId("section-tracking") }

    internal val photoDetails = Details(
        "Photo",
        VerticalLayout(existingImageLabel, currentImageDisplay, uploadComponent, uploadHint, uploadErrorLabel)
            .apply {
                isPadding = false
                isSpacing = false
                // Only present when the AI feature is enabled; sits below the upload as a follow-up action.
                autoFillButton?.let { add(it) }
            },
    ).apply { setId("section-photo") }

    init {
        isPadding = false

        uploadComponent.addSucceededListener { event ->
            val data = buffer.inputStream.readBytes()
            val mimeType = event.mimeType ?: ""
            when {
                mimeType !in ALLOWED_TYPES -> { clearStagedImage(); showUploadError("Invalid format. Allowed: JPEG, PNG, WebP.") }
                data.size > MAX_IMAGE_BYTES -> { clearStagedImage(); showUploadError("File exceeds the 5 MB maximum.") }
                else -> {
                    pendingImageData = data
                    pendingImageMimeType = mimeType
                    uploadErrorLabel.isVisible = false
                    autoFillButton?.isEnabled = true
                }
            }
        }
        uploadComponent.addFileRejectedListener { event ->
            clearStagedImage()
            showUploadError(event.errorMessage)
        }
        // No `capture` attribute — iOS shows its native picker ("Take Photo", "Photo Library", "Files")
        // giving the user both camera and gallery. Setting capture="environment" bypasses gallery entirely.

        tagsField.addCustomValueSetListener { event ->
            val raw = event.detail?.trim()?.lowercase() ?: return@addCustomValueSetListener
            when {
                raw.isBlank() -> return@addCustomValueSetListener
                raw.contains(",") -> NotificationHelper.error("Tags can't contain commas")
                raw.length > MAX_TAG_LENGTH -> NotificationHelper.error("Tag must be $MAX_TAG_LENGTH characters or fewer")
                tagsField.value.size >= MAX_TAG_COUNT -> NotificationHelper.error("Maximum $MAX_TAG_COUNT tags allowed")
                else -> {
                    val updated = tagsField.value.toMutableSet().also { it.add(raw) }
                    tagsField.value = updated
                }
            }
        }
        tagsField.addValueChangeListener { event ->
            if ((event.value?.size ?: 0) > MAX_TAG_COUNT) {
                tagsField.value = event.oldValue
                NotificationHelper.error("Maximum $MAX_TAG_COUNT tags allowed")
            }
        }

        configureBinder()

        // An AI-suggested field stops being "suggested" the moment the user edits it themselves
        // (client-originated change), so the accent only flags values the user hasn't yet vetted.
        if (aiExtractionService != null) {
            listOf(nameField, roasterField, originField, notesField).forEach { it.clearMarkOnEdit() }
            roastLevelField.clearMarkOnEdit()
            processField.clearMarkOnEdit()
            weightField.clearMarkOnEdit()
            priceField.clearMarkOnEdit()
            roastDateField.clearMarkOnEdit()
        }

        // Essentials are always visible; everything optional is tucked into collapsible sections so
        // adding a bean isn't a 16-field wall. Required fields are never hidden behind a collapse.
        val essentials = FormLayout(
            nameField, roasterField, originField,
            roastLevelField, processField, roastProfileField,
            priceField, weightField,
            purchaseDateField, roastDateField,
        )

        val buttons = if (onCancel != null) {
            HorizontalLayout(saveButton, Button("Cancel") { onCancel.invoke() })
        } else {
            HorizontalLayout(saveButton)
        }

        // The Link field gains an "Auto-fill from link" action when AI is on; otherwise it stands alone.
        val linkSection: Component = autoFillFromLinkButton?.let {
            VerticalLayout(linkField, it).apply { isPadding = false; isSpacing = false; width = "100%" }
        } ?: linkField

        // Photo leads the form: uploading the bag is the natural first step and — when AI is enabled —
        // powers "Auto-fill from photo", so it sits up front rather than trailing as an afterthought.
        add(photoDetails, essentials, linkSection, tastingDetails, trackingDetails, buttons)
    }

    private fun showUploadError(message: String) {
        uploadErrorLabel.text = message
        uploadErrorLabel.isVisible = true
    }

    private fun clearStagedImage() {
        pendingImageData = null
        pendingImageMimeType = null
        autoFillButton?.isEnabled = false
    }

    /**
     * Sends the staged photo to the model and fills in blank fields. Runs synchronously (the call is
     * short and single-user); the browser shows Vaadin's built-in progress indicator. [AiExtractionService]
     * already maps every failure to `null`, so a miss simply asks the user to fill it in manually.
     */
    private fun autoFillFromPhoto() {
        val service = aiExtractionService ?: return
        val bytes = pendingImageData
        if (bytes == null) {
            NotificationHelper.error("Upload a photo first")
            return
        }
        val extraction = runBlocking { service.extractFromImage(bytes, pendingImageMimeType ?: DEFAULT_IMAGE_MIME) }
        if (extraction != null) {
            applyExtraction(extraction)
            NotificationHelper.success("Filled in from the photo — please review the fields")
        } else {
            NotificationHelper.error("Couldn't read that photo — please fill it in manually")
        }
    }

    /**
     * Fetches the URL in the Link field, extracts bean fields, and fills blanks. Runs synchronously like
     * the photo path; [AiExtractionService] maps every failure to `null`.
     */
    private fun autoFillFromLink() {
        val service = aiExtractionService ?: return
        val url = linkField.value.trim()
        if (url.isBlank()) {
            NotificationHelper.error("Enter a link first")
            return
        }
        val extraction = runBlocking { service.extractFromUrl(url) }
        if (extraction != null) {
            applyExtraction(extraction)
            NotificationHelper.success("Filled in from the link — please review the fields")
        } else {
            NotificationHelper.error("Couldn't read that link — please fill it in manually")
        }
    }

    /**
     * Pre-fills only currently-blank fields from [extraction] (never overwrites what the user typed) and
     * flags each filled field as AI-suggested. The roast date is filled when the bag prints one; the
     * purchase date, rating, tags, and the image itself are intentionally left to the user.
     */
    internal fun applyExtraction(extraction: BeanExtraction) {
        fillTextIfBlank(nameField, extraction.name)
        fillTextIfBlank(roasterField, extraction.roaster)
        fillTextIfBlank(originField, extraction.origin)
        if (roastLevelField.value == null && extraction.roastLevel != null) {
            roastLevelField.value = extraction.roastLevel
            markAi(roastLevelField)
        }
        if (extraction.roastProfile != null) {
            roastProfileField.value = extraction.roastProfile
            markAi(roastProfileField)
        }
        if (processField.value == null && extraction.process != null) {
            processField.value = extraction.process
            markAi(processField)
        }
        val roastDate = parseIsoDate(extraction.roastDate)
        if (roastDateField.value == null && roastDate != null) {
            roastDateField.value = roastDate
            markAi(roastDateField)
        }
        if (weightField.value == null && extraction.weightGrams != null) {
            weightField.value = extraction.weightGrams
            markAi(weightField)
        }
        if (priceField.value == null && extraction.price != null) {
            priceField.value = BigDecimal.valueOf(extraction.price)
            markAi(priceField)
        }
        if (notesField.value.isBlank() && !extraction.notes.isNullOrBlank()) {
            notesField.value = extraction.notes
            markAi(notesField)
            tastingDetails.isOpened = true
        }
    }

    private fun fillTextIfBlank(field: TextField, value: String?) {
        if (field.value.isBlank() && !value.isNullOrBlank()) {
            field.value = value
            markAi(field)
        }
    }

    /** Parses an ISO-8601 (yyyy-MM-dd) date, returning null for absent or malformed values. */
    private fun parseIsoDate(value: String?): LocalDate? =
        value?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun markAi(field: HasStyle) = field.addClassName(AI_CLASS)

    /** Drops the AI-suggested accent the first time the user changes the value themselves. */
    private fun <C> C.clearMarkOnEdit() where C : HasValue<*, *>, C : HasStyle =
        addValueChangeListener { if (it.isFromClient) removeClassName(AI_CLASS) }

    private fun clearAiMarks() = listOf<HasStyle>(
        nameField, roasterField, originField, notesField,
        roastLevelField, processField, weightField, priceField, roastDateField,
    ).forEach { it.removeClassName(AI_CLASS) }

    private fun configureBinder() {
        binder.forField(nameField)
            .withValidator({ it.isNotBlank() }, REQUIRED)
            .bind({ it.name }, { b, v -> b.name = v })
        binder.forField(roasterField)
            .withValidator({ it.isNotBlank() }, REQUIRED)
            .bind({ it.roaster }, { b, v -> b.roaster = v })
        binder.forField(originField)
            .withValidator({ it.isNotBlank() }, REQUIRED)
            .bind({ it.origin }, { b, v -> b.origin = v })
        binder.forField(priceField)
            .withValidator({ it != null }, REQUIRED)
            .withValidator({ it == null || it > BigDecimal.ZERO }, "Must be greater than 0")
            .bind({ it.price }, { b, v -> b.price = v })
        binder.forField(weightField)
            .withValidator({ it != null }, REQUIRED)
            .withValidator({ it == null || it > 0 }, "Must be greater than 0")
            .bind({ it.weightGrams }, { b, v -> b.weightGrams = v })
        binder.forField(purchaseDateField)
            .asRequired(REQUIRED)
            .bind({ it.purchaseDate }, { b, v -> b.purchaseDate = v })
        binder.forField(roastDateField)
            .asRequired(REQUIRED)
            .bind({ it.roastDate }, { b, v -> b.roastDate = v })
        binder.forField(roastLevelField)
            .asRequired(REQUIRED)
            .bind({ it.roastLevel }, { b, v -> b.roastLevel = v })
        binder.forField(processField)
            .asRequired(REQUIRED)
            .bind({ it.process }, { b, v -> b.process = v })
        binder.forField(roastProfileField)
            .asRequired(REQUIRED)
            .bind({ it.roastProfile }, { b, v -> b.roastProfile = v })
        binder.forField(notesField)
            .bind({ it.notes }, { b, v -> b.notes = v })
        binder.forField(tagsField)
            .bind({ it.tags }, { b, v -> b.tags = v ?: emptySet() })
        binder.forField(grindSettingsField)
            .bind({ it.grindSettings }, { b, v -> b.grindSettings = v })
        binder.forField(linkField)
            .bind({ it.url }, { b, v -> b.url = v })
        binder.forField(ratingField)
            .bind({ it.rating }, { b, v -> b.rating = v })
        binder.forField(openedDateField)
            .bind({ it.openedDate }, { b, v -> b.openedDate = v })
        val finishedBinding = binder.forField(finishedDateField)
            // Cross-field: a bag can't be finished before it was opened. Reads the opened field's
            // current value (the binder is non-buffered) so analytics never ingests a negative pace.
            .withValidator(
                { finished ->
                    val opened = openedDateField.value
                    finished == null || opened == null || !finished.isBefore(opened)
                },
                "Can't be before the opened date",
            )
            .bind({ it.finishedDate }, { b, v -> b.finishedDate = v })
        // Re-check only the finished field when the opened date changes (validating the whole binder
        // here would prematurely flag still-empty required fields on the create form).
        openedDateField.addValueChangeListener {
            if (finishedDateField.value != null) finishedBinding.validate()
        }
    }

    fun openForCreate() {
        editingId = null
        clearStagedImage()
        clearAiMarks()
        binder.bean = PurchaseFormBean()
        tagsField.setItems(getAllTags())
        clearUploadState()
        currentImageDisplay.isVisible = false
        existingImageLabel.isVisible = false
        uploadErrorLabel.isVisible = false
        collapseOptionalSections()
        // Open on create so the upload (and the AI auto-fill action) is visible up front, not buried.
        photoDetails.isOpened = true
    }

    private fun collapseOptionalSections() {
        tastingDetails.isOpened = false
        trackingDetails.isOpened = false
    }

    fun openForEdit(purchase: BeanPurchase) {
        editingId = purchase.id
        clearStagedImage()
        clearAiMarks()
        clearUploadState()
        binder.bean = PurchaseFormBean().apply {
            name = purchase.name
            roaster = purchase.roaster
            origin = purchase.origin
            price = purchase.price
            weightGrams = purchase.weightGrams
            purchaseDate = purchase.purchaseDate
            roastDate = purchase.roastDate
            roastLevel = purchase.roastLevel
            process = purchase.process
            notes = purchase.notes ?: ""
            grindSettings = purchase.grindSettings ?: ""
            imageData = purchase.imageData
            rating = purchase.rating
            openedDate = purchase.openedDate
            finishedDate = purchase.finishedDate
            roastProfile = purchase.roastProfile
            usedAs = purchase.usedAs
            tags = purchase.tags
            url = purchase.url ?: ""
        }
        tagsField.setItems(getAllTags())
        if (purchase.imageData != null) {
            currentImageDisplay.setSrc(
                com.vaadin.flow.server.streams.InputStreamDownloadHandler { _ ->
                    com.vaadin.flow.server.streams.DownloadResponse(
                        ByteArrayInputStream(purchase.imageData),
                        "photo.jpg", "image/jpeg",
                        purchase.imageData.size.toLong()
                    )
                }.inline()
            )
            currentImageDisplay.isVisible = true
            existingImageLabel.isVisible = true
        } else {
            currentImageDisplay.isVisible = false
            existingImageLabel.isVisible = false
        }
        uploadErrorLabel.isVisible = false
        // Reveal optional sections that already hold data so editing doesn't hide existing values.
        tastingDetails.isOpened = purchase.rating != null || purchase.tags.isNotEmpty() ||
            !purchase.grindSettings.isNullOrBlank() || !purchase.notes.isNullOrBlank()
        trackingDetails.isOpened = purchase.openedDate != null || purchase.finishedDate != null
        photoDetails.isOpened = purchase.imageData != null
    }

    private fun clearUploadState() {
        // Clear the web component's displayed file list so the previous upload
        // is not visible when the dialog opens for a different purchase.
        uploadComponent.clearFileList()
    }

    /**
     * Opens the form for re-purchasing, pre-filling only the profile fields of [source] (name, roaster,
     * origin, roast, notes, tags, link, image) while leaving transaction fields empty.
     */
    fun openWithProfile(source: BeanPurchase) {
        // Reset all fields to empty first, then overlay profile fields via direct field assignment (RULE-10)
        openForCreate()
        // Set field values directly — Vaadin's non-buffered binder automatically syncs to the bean
        nameField.value = source.name
        roasterField.value = source.roaster
        originField.value = source.origin
        roastLevelField.value = source.roastLevel
        processField.value = source.process
        notesField.value = source.notes ?: ""
        grindSettingsField.value = source.grindSettings ?: ""
        // Image bytes stored in pendingImageData (not via Upload component)
        pendingImageData = source.imageData
        roastProfileField.value = source.roastProfile
        binder.bean.usedAs = source.usedAs
        tagsField.value = source.tags
        // The product/roaster link is profile data — a re-purchase points at the same page.
        linkField.value = source.url ?: ""
        // Transaction fields (price, weight, dates, rating) remain empty from openForCreate()
        // Reveal the tasting section so the carried-over notes/tags/grind are visible.
        tastingDetails.isOpened = source.tags.isNotEmpty() ||
            !source.notes.isNullOrBlank() || !source.grindSettings.isNullOrBlank()
    }
}
