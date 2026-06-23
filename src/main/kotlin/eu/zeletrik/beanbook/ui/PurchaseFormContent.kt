package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
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
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.RoastProfile
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.util.UUID

private const val MAX_IMAGE_BYTES = 5_242_880
private val ALLOWED_TYPES = setOf("image/jpeg", "image/png", "image/webp")


private const val MAX_TAG_LENGTH = 20
private const val MAX_TAG_COUNT = 10

class PurchaseFormContent(
    private val onSave: (bean: PurchaseFormBean, existingId: UUID?) -> Unit,
    private val onCancel: (() -> Unit)? = null,
    private val getAllTags: () -> Set<String> = { emptySet() },
) : VerticalLayout() {

    internal val nameField = TextField("Name").also { it.setId("field-name") }
    internal val roasterField = TextField("Roaster").also { it.setId("field-roaster") }
    internal val originField = TextField("Origin").also { it.setId("field-origin") }
    internal val priceField = BigDecimalField("Price per unit").also { it.setId("field-price") }
    internal val weightField = IntegerField("Weight (g)").also { it.setId("field-weight") }
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
        it.setItemLabelGenerator { rp -> rp.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
    }
    internal val notesField = TextArea("Notes").also { it.setId("field-notes") }
    internal val tagsField = MultiSelectComboBox<String>("Tags").also {
        it.setId("field-tags")
        it.setAllowCustomValue(true)
    }
    internal val grindSettingsField = TextField("Grind settings").also { it.setId("field-grind") }

    // Rating: 1–5, null = not rated
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
    private val uploadErrorLabel = Span().also {
        it.setId("upload-error")
        it.isVisible = false
        it.style["color"] = "var(--lumo-error-color)"
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

    init {
        isPadding = false

        uploadComponent.addSucceededListener { event ->
            val data = buffer.inputStream.readBytes()
            val mimeType = event.mimeType ?: ""
            when {
                mimeType !in ALLOWED_TYPES -> { pendingImageData = null; showUploadError("Invalid format. Allowed: JPEG, PNG, WebP.") }
                data.size > MAX_IMAGE_BYTES -> { pendingImageData = null; showUploadError("File exceeds the 5 MB maximum.") }
                else -> { pendingImageData = data; uploadErrorLabel.isVisible = false }
            }
        }
        uploadComponent.addFileRejectedListener { event ->
            pendingImageData = null
            showUploadError(event.errorMessage)
        }
        // No `capture` attribute — iOS shows its native picker ("Take Photo", "Photo Library", "Files")
        // giving the user both camera and gallery. Setting capture="environment" bypasses gallery entirely.

        tagsField.addCustomValueSetListener { event ->
            val raw = event.detail?.trim()?.lowercase() ?: return@addCustomValueSetListener
            when {
                raw.isBlank() -> return@addCustomValueSetListener
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

        val coreForm = FormLayout(
            nameField, roasterField, originField,
            priceField, weightField,
            purchaseDateField, roastDateField,
            roastLevelField, processField,
            roastProfileField,
            notesField, tagsField, grindSettingsField, ratingField,
        )

        val stateForm = VerticalLayout().apply {
            isPadding = false
            isSpacing = false
            add(H4("State tracker").apply { style["margin"] = "0.5rem 0 0.25rem 0" })
            add(FormLayout(openedDateField, finishedDateField))
        }

        val imageSection = VerticalLayout(
            existingImageLabel, currentImageDisplay, uploadComponent, uploadErrorLabel
        ).apply { isPadding = false; isSpacing = false }

        val buttons = if (onCancel != null) {
            HorizontalLayout(saveButton, Button("Cancel") { onCancel.invoke() })
        } else {
            HorizontalLayout(saveButton)
        }

        add(coreForm, stateForm, imageSection, buttons)
    }

    private fun showUploadError(message: String) {
        uploadErrorLabel.text = message
        uploadErrorLabel.isVisible = true
    }

    private fun configureBinder() {
        binder.forField(nameField)
            .withValidator({ it.isNotBlank() }, "Required")
            .bind({ it.name }, { b, v -> b.name = v })
        binder.forField(roasterField)
            .withValidator({ it.isNotBlank() }, "Required")
            .bind({ it.roaster }, { b, v -> b.roaster = v })
        binder.forField(originField)
            .withValidator({ it.isNotBlank() }, "Required")
            .bind({ it.origin }, { b, v -> b.origin = v })
        binder.forField(priceField)
            .withValidator({ it != null }, "Required")
            .withValidator({ it == null || it > BigDecimal.ZERO }, "Must be greater than 0")
            .bind({ it.pricePerUnit }, { b, v -> b.pricePerUnit = v })
        binder.forField(weightField)
            .withValidator({ it != null }, "Required")
            .withValidator({ it == null || it > 0 }, "Must be greater than 0")
            .bind({ it.weightGrams }, { b, v -> b.weightGrams = v })
        binder.forField(purchaseDateField)
            .asRequired("Required")
            .bind({ it.purchaseDate }, { b, v -> b.purchaseDate = v })
        binder.forField(roastDateField)
            .asRequired("Required")
            .bind({ it.roastDate }, { b, v -> b.roastDate = v })
        binder.forField(roastLevelField)
            .asRequired("Required")
            .bind({ it.roastLevel }, { b, v -> b.roastLevel = v })
        binder.forField(processField)
            .asRequired("Required")
            .bind({ it.process }, { b, v -> b.process = v })
        binder.forField(roastProfileField)
            .asRequired("Required")
            .bind({ it.roastProfile }, { b, v -> b.roastProfile = v })
        binder.forField(notesField)
            .bind({ it.notes }, { b, v -> b.notes = v })
        binder.forField(tagsField)
            .bind({ it.tags.toSet() }, { b, v -> b.tags = v?.toList() ?: emptyList() })
        binder.forField(grindSettingsField)
            .bind({ it.grindSettings }, { b, v -> b.grindSettings = v })
        binder.forField(ratingField)
            .bind({ it.rating }, { b, v -> b.rating = v })
        binder.forField(openedDateField)
            .bind({ it.openedDate }, { b, v -> b.openedDate = v })
        binder.forField(finishedDateField)
            .bind({ it.finishedDate }, { b, v -> b.finishedDate = v })
    }

    fun openForCreate() {
        editingId = null
        pendingImageData = null
        binder.bean = PurchaseFormBean()
        tagsField.setItems(getAllTags())
        clearUploadState()
        currentImageDisplay.isVisible = false
        existingImageLabel.isVisible = false
        uploadErrorLabel.isVisible = false
    }

    fun openForEdit(purchase: BeanPurchase) {
        editingId = purchase.id
        pendingImageData = null
        clearUploadState()
        binder.bean = PurchaseFormBean().apply {
            name = purchase.name
            roaster = purchase.roaster
            origin = purchase.origin
            pricePerUnit = purchase.pricePerUnit
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
    }

    private fun clearUploadState() {
        // Clear the web component's displayed file list so the previous upload
        // is not visible when the dialog opens for a different purchase.
        uploadComponent.element.executeJs("this.files = []")
    }

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
        tagsField.value = source.tags.toSet()
        // Transaction fields (price, weight, dates, rating) remain empty from openForCreate()
    }
}
