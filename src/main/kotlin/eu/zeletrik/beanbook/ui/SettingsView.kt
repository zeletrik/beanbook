package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.component.upload.receivers.MemoryBuffer
import com.vaadin.flow.server.StreamResource
import eu.zeletrik.beanbook.preferences.PreferencesService
import eu.zeletrik.beanbook.backup.ExportService
import eu.zeletrik.beanbook.backup.ImportResult
import eu.zeletrik.beanbook.backup.ImportService
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.time.LocalDate

private val log = LoggerFactory.getLogger(SettingsView::class.java)

/** 20 MB cap on the import upload — backups can contain many base64-encoded images. */
private const val MAX_IMPORT_BYTES = 20 * 1024 * 1024

/** Settings view exposing data export, import, and user preferences such as the display currency. */
class SettingsView(
    private val exportService: ExportService,
    private val importService: ImportService,
    private val preferencesService: PreferencesService,
    private val onImportComplete: () -> Unit = {},
    private val onCurrencyChanged: () -> Unit = {},
) : VerticalLayout() {

    private val exportAnchor: Anchor

    init {
        isPadding = true
        isSpacing = true

        // ── Export ────────────────────────────────────────────────
        // The supplier runs on a background request thread, so it must not touch the UI. On
        // failure we log and rethrow rather than serving a 0-byte file as a successful download.
        val exportResource = StreamResource("beanbook-export-${LocalDate.now()}.json") {
            try {
                ByteArrayInputStream(exportService.generateJson())
            } catch (e: Exception) {
                log.error("Export failed while generating JSON", e)
                throw e
            }
        }

        exportAnchor = Anchor(exportResource, "Export Data").apply {
            setId("export-data-btn")
            element.setAttribute("download", true)
            style["display"] = "inline-flex"
            style["align-items"] = "center"
            style["padding"] = "0.5rem 1rem"
            style["background"] = "var(--lumo-primary-color)"
            style["color"] = "var(--lumo-primary-contrast-color)"
            style["border-radius"] = "var(--lumo-border-radius-m)"
            style["text-decoration"] = "none"
            style["font-weight"] = "600"
        }

        add(exportAnchor)

        // ── Import ────────────────────────────────────────────────
        val importBuffer = MemoryBuffer()
        val importUpload = Upload(importBuffer).apply {
            setId("import-upload")
            setAcceptedFileTypes("application/json", ".json")
            setMaxFileSize(MAX_IMPORT_BYTES)
            addSucceededListener {
                try {
                    val bytes = importBuffer.inputStream.use { it.readBytes() }
                    onImportFinished(importService.import(bytes))
                } catch (e: Exception) {
                    log.error("Import failed while reading or applying the uploaded file", e)
                    NotificationHelper.error("Import failed — please try again")
                }
            }
            addFileRejectedListener {
                NotificationHelper.error("Import failed — please select a .json file under 20 MB")
            }
        }

        add(Span("Import Data").apply {
            style["font-weight"] = "600"
            style["font-size"] = "var(--lumo-font-size-s)"
        })
        add(importUpload)

        add(Hr())

        // ── Preferences ───────────────────────────────────────────
        add(H3("Preferences"))

        add(Span("Currency").apply {
            style["font-size"] = "var(--lumo-font-size-s)"
            style["color"] = "var(--lumo-secondary-text-color)"
        })

        val currencyOptions = listOf("€", "$", "£", "¥", "CHF")
        val currencySelect = Select<String>().apply {
            setId("currency-select")
            setItemLabelGenerator { it }
            setItems(*currencyOptions.toTypedArray())
            value = preferencesService.getCurrency()
            addValueChangeListener { event ->
                if (event.value != null) {
                    preferencesService.setCurrency(event.value)
                    onCurrencyChanged()
                }
            }
        }
        add(currencySelect)
    }

    /** Surfaces an import result to the user. Extracted so the notification wiring is testable. */
    internal fun onImportFinished(result: ImportResult) {
        if (!result.success) {
            NotificationHelper.error("Import failed — ${result.error ?: "invalid or empty file"}")
            return
        }
        val msg = buildString {
            append("Imported ")
            if (result.purchases > 0) append("${result.purchases} beans")
            if (result.purchases > 0 && result.wishlist > 0) append(", ")
            if (result.wishlist > 0) append("${result.wishlist} wishlist items")
            if (result.purchases == 0 && result.wishlist == 0) append("0 records")
            if (result.skipped > 0) append(" (${result.skipped} skipped)")
        }
        NotificationHelper.success(msg)
        onImportComplete()
    }
}
