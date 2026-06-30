package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasStyle
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.select.Select
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
    private val getAllRoasters: () -> Set<String> = { emptySet() },
    /** When present, enables "Auto-fill from photo"; null hides the action entirely (feature off). */
    private val aiExtractionService: AiExtractionService? = null,
) : VerticalLayout() {

    internal val nameField = TextField("Name").also { it.setId("field-name"); it.isRequiredIndicatorVisible = true }
    // Typeahead over roasters already recorded (still accepts a brand-new value) so naming stays
    // consistent and entry is faster (#21). Suggestions are seeded in openForCreate / openForEdit.
    internal val roasterField = ComboBox<String>("Roaster").also { combo ->
        combo.setId("field-roaster")
        combo.isRequiredIndicatorVisible = true
        combo.isAllowCustomValue = true
        combo.isClearButtonVisible = true
        combo.addCustomValueSetListener { event -> combo.value = event.detail }
    }
    internal val originField = TextField("Origin").also { it.setId("field-origin"); it.isRequiredIndicatorVisible = true }
    /** Optional second-level origin (region / sub-origin), e.g. "Huila" for a Colombia bean. */
    internal val regionField = TextField("Region").also { it.setId("field-region") }
    // A plain text field with decimal inputmode (not BigDecimalField): iOS shows a numeric keypad with a
    // separator, and the binder converter accepts both "." and "," — so entry works whatever separator the
    // device's keypad produces. BigDecimalField parses with one fixed locale and rejected the other (#18).
    internal val priceField = TextField("Price per unit").also {
        it.setId("field-price")
        it.isRequiredIndicatorVisible = true
        it.element.setAttribute("inputmode", "decimal")
    }
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
        it.setAlt("Current bean photo")
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

    // ── Always-open card sections (coherent with the rest of the app — see UiComponents.sectionCard) ──
    // Photo leads: uploading the bag is the natural first step and — when AI is on — powers auto-fill.
    internal val photoSection = sectionCard(
        VaadinIcon.CAMERA, "#2e8b57",
        "Photo", if (autoFillButton != null) "Add a bag photo to auto-fill the details." else "Add a bag photo.",
        *listOfNotNull<Component>(
            existingImageLabel, currentImageDisplay, uploadComponent, uploadHint, uploadErrorLabel,
            autoFillButton?.apply { style["align-self"] = "flex-start" },
        ).toTypedArray(),
    ).apply { setId("section-photo") }

    private val beanSection = sectionCard(
        VaadinIcon.COFFEE, "var(--lumo-primary-color)",
        "Bean", "What bean is this?",
        *listOfNotNull<Component>(
            FormLayout(nameField, roasterField, originField, regionField),
            linkField,
            autoFillFromLinkButton?.apply { style["align-self"] = "flex-start" },
        ).toTypedArray(),
    ).apply { setId("section-bean") }

    private val roastSection = sectionCard(
        VaadinIcon.FIRE, "#c25e00",
        "Roast", "How it was roasted.",
        FormLayout(roastLevelField, processField, roastProfileField, roastDateField),
    ).apply { setId("section-roast") }

    private val purchaseSection = sectionCard(
        VaadinIcon.CART, "#e6a817",
        "Purchase", "What you paid, and when.",
        FormLayout(priceField, weightField, purchaseDateField),
    ).apply { setId("section-purchase") }

    private val tastingSection = sectionCard(
        VaadinIcon.STAR, "#7c4dff",
        "Tasting notes & tags", "Your rating, tags, grind, and notes.",
        FormLayout(ratingField, tagsField, grindSettingsField, notesField).apply {
            setColspan(tagsField, 2); setColspan(notesField, 2)
        },
    ).apply { setId("section-tasting") }

    private val trackingSection = sectionCard(
        VaadinIcon.CLOCK, "#2e7d9c",
        "Bag tracking", "When you opened and finished it.",
        FormLayout(openedDateField, finishedDateField),
    ).apply { setId("section-tracking") }

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
            listOf(nameField, roasterField, originField, regionField, notesField).forEach { it.clearMarkOnEdit() }
            roastLevelField.clearMarkOnEdit()
            roastProfileField.clearMarkOnEdit()
            processField.clearMarkOnEdit()
            weightField.clearMarkOnEdit()
            priceField.clearMarkOnEdit()
            roastDateField.clearMarkOnEdit()
        }

        // A floating Save bar pinned to the bottom of the scroll area, so saving never requires
        // scrolling past the always-open cards. Cancel appears only when hosted in the edit dialog.
        saveButton.style["flex"] = "1"; saveButton.style["min-height"] = "44px"
        val actionBar = HorizontalLayout(saveButton).apply {
            setId("form-actions")
            width = "100%"; isPadding = false; isSpacing = true
            style["position"] = "sticky"; style["bottom"] = "0"; style["z-index"] = "5"
            style["margin-top"] = "0.5rem"; style["padding"] = "0.75rem 0"
            style["align-items"] = "center"
            style["background"] = "var(--lumo-base-color)"
            style["border-top"] = "1px solid var(--lumo-contrast-10pct)"
            style["box-shadow"] = "0 -2px 8px rgba(0,0,0,0.06)"
            onCancel?.let { cancel -> add(Button("Cancel") { cancel.invoke() }.apply { style["min-height"] = "44px" }) }
        }

        add(photoSection, beanSection, roastSection, purchaseSection, tastingSection, trackingSection, actionBar)
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
        if (roasterField.value.isNullOrBlank() && !extraction.roaster.isNullOrBlank()) {
            roasterField.value = extraction.roaster
            markAi(roasterField)
        }
        fillTextIfBlank(originField, extraction.origin)
        fillTextIfBlank(regionField, extraction.region)
        if (roastLevelField.value == null && extraction.roastLevel != null) {
            roastLevelField.value = extraction.roastLevel
            markAi(roastLevelField)
        }
        // The roast-profile Select is never blank (defaults to OMNI), so "fill only blanks" can't apply.
        // Override only while it's still at the OMNI default — a profile the user already chose is kept.
        if (roastProfileField.value == RoastProfile.OMNI &&
            extraction.roastProfile != null && extraction.roastProfile != RoastProfile.OMNI
        ) {
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
        if (priceField.value.isBlank() && extraction.price != null) {
            priceField.value = BigDecimal.valueOf(extraction.price).toPlainString()
            markAi(priceField)
        }
        if (notesField.value.isBlank() && !extraction.notes.isNullOrBlank()) {
            notesField.value = extraction.notes
            markAi(notesField)
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
        nameField, roasterField, originField, regionField, notesField,
        roastLevelField, roastProfileField, processField, weightField, priceField, roastDateField,
    ).forEach { it.removeClassName(AI_CLASS) }

    private fun configureBinder() {
        binder.forField(nameField)
            .withValidator({ it.isNotBlank() }, REQUIRED)
            .bind({ it.name }, { b, v -> b.name = v })
        binder.forField(roasterField)
            .withValidator({ !it.isNullOrBlank() }, REQUIRED)
            .bind({ it.roaster }, { b, v -> b.roaster = v ?: "" })
        binder.forField(originField)
            .withValidator({ it.isNotBlank() }, REQUIRED)
            .bind({ it.origin }, { b, v -> b.origin = v })
        binder.forField(regionField) // optional second-level origin
            .bind({ it.region }, { b, v -> b.region = v })
        binder.forField(priceField)
            // Accept both "." and "," as the decimal separator (the iOS keypad emits the device's), and
            // present the stored value with a dot. Bad input fails conversion with a clear message.
            .withConverter(
                { text -> text.trim().replace(',', '.').takeIf(String::isNotEmpty)?.let(::BigDecimal) },
                { value: BigDecimal? -> value?.toPlainString() ?: "" },
                "Enter a valid price",
            )
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
        roasterField.setItems(getAllRoasters())
        binder.bean = PurchaseFormBean()
        tagsField.setItems(getAllTags())
        clearUploadState()
        currentImageDisplay.isVisible = false
        existingImageLabel.isVisible = false
        uploadErrorLabel.isVisible = false
    }

    fun openForEdit(purchase: BeanPurchase) {
        editingId = purchase.id
        clearStagedImage()
        clearAiMarks()
        clearUploadState()
        roasterField.setItems(getAllRoasters())
        // Seed the tag items (incl. this purchase's own tags) BEFORE setting the bean: the binder pushes
        // the value into the MultiSelectComboBox on setBean, and it rejects values absent from its items.
        tagsField.setItems(getAllTags() + purchase.tags)
        binder.bean = PurchaseFormBean().apply {
            name = purchase.name
            roaster = purchase.roaster
            origin = purchase.origin
            region = purchase.region ?: ""
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
    }

    private fun clearUploadState() {
        // Clear the web component's displayed file list so the previous upload
        // is not visible when the dialog opens for a different purchase.
        uploadComponent.clearFileList()
    }

    /**
     * Opens the form for re-purchasing, pre-filling only the profile fields of [source] (name, roaster,
     * origin, region, roast, notes, tags, link, image) while leaving transaction fields empty.
     */
    fun openWithProfile(source: BeanPurchase) {
        // Reset all fields to empty first, then overlay profile fields via direct field assignment (RULE-10)
        openForCreate()
        // Set field values directly — Vaadin's non-buffered binder automatically syncs to the bean
        nameField.value = source.name
        roasterField.value = source.roaster
        originField.value = source.origin
        regionField.value = source.region ?: ""
        roastLevelField.value = source.roastLevel
        processField.value = source.process
        notesField.value = source.notes ?: ""
        grindSettingsField.value = source.grindSettings ?: ""
        // Image bytes stored in pendingImageData (not via Upload component)
        pendingImageData = source.imageData
        roastProfileField.value = source.roastProfile
        binder.bean.usedAs = source.usedAs
        // Ensure the source's tags are valid items before selecting them (see openForEdit).
        tagsField.setItems(getAllTags() + source.tags)
        tagsField.value = source.tags
        // The product/roaster link is profile data — a re-purchase points at the same page.
        linkField.value = source.url ?: ""
        // Transaction fields (price, weight, dates, rating) remain empty from openForCreate()
    }
}
