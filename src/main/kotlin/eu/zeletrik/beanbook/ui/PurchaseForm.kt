package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.dialog.Dialog
import eu.zeletrik.beanbook.ai.AiExtractionService
import eu.zeletrik.beanbook.beans.BeanPurchase
import java.util.UUID

/** Modal [Dialog] wrapping [PurchaseFormContent] for creating, editing, or duplicating a [BeanPurchase]. */
class PurchaseForm(
    onSave: (bean: PurchaseFormBean, existingId: UUID?) -> Unit,
    getAllTags: () -> Set<String> = { emptySet() },
    aiExtractionService: AiExtractionService? = null,
) : Dialog() {

    private val content = PurchaseFormContent(
        onSave = { bean, id -> onSave(bean, id); close() },
        onCancel = { close() },
        getAllTags = getAllTags,
        aiExtractionService = aiExtractionService,
    )

    /** Exposes the underlying [content] fields for tests. */
    internal val nameField get() = content.nameField
    internal val roasterField get() = content.roasterField
    internal val originField get() = content.originField
    internal val priceField get() = content.priceField
    internal val weightField get() = content.weightField
    internal val purchaseDateField get() = content.purchaseDateField
    internal val roastDateField get() = content.roastDateField
    internal val roastLevelField get() = content.roastLevelField
    internal val processField get() = content.processField
    internal val roastProfileField get() = content.roastProfileField
    internal val tagsField get() = content.tagsField
    internal val notesField get() = content.notesField
    internal val grindSettingsField get() = content.grindSettingsField
    internal val saveButton get() = content.saveButton
    internal val uploadComponent get() = content.uploadComponent
    internal val autoFillButton get() = content.autoFillButton
    internal var pendingImageData: ByteArray?
        get() = content.pendingImageData
        set(v) { content.pendingImageData = v }
    internal val currentImageDisplay get() = content.currentImageDisplay

    init {
        add(content)
    }

    fun openForCreate() {
        setHeaderTitle("New Purchase")
        content.openForCreate()
        open()
    }

    fun openForEdit(purchase: BeanPurchase) {
        setHeaderTitle("Edit Purchase")
        content.openForEdit(purchase)
        open()
    }

    fun openWithProfile(source: BeanPurchase) = content.openWithProfile(source)
}
