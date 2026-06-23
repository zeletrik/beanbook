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
import eu.zeletrik.beanbook.PreferencesService
import eu.zeletrik.beanbook.beans.ExportService
import eu.zeletrik.beanbook.beans.ImportService
import java.io.ByteArrayInputStream
import java.time.LocalDate

class SettingsView(
    private val exportService: ExportService,
    private val importService: ImportService,
    private val preferencesService: PreferencesService,
    private val onImportComplete: () -> Unit = {},
) : VerticalLayout() {

    private val exportAnchor: Anchor

    init {
        isPadding = true
        isSpacing = true

        // ── Export ────────────────────────────────────────────────
        val exportResource = StreamResource("beanbook-export-${LocalDate.now()}.json") {
            try {
                ByteArrayInputStream(exportService.generateJson())
            } catch (e: Exception) {
                NotificationHelper.error("Failed to export — please try again")
                ByteArrayInputStream(ByteArray(0))
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
            addSucceededListener {
                try {
                    val bytes = importBuffer.inputStream.readBytes()
                    val result = importService.import(bytes)
                    if (!result.success) {
                        NotificationHelper.error("Import failed — invalid or empty file")
                    } else {
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
                } catch (e: Exception) {
                    NotificationHelper.error("Import failed — please try again")
                }
            }
            addFileRejectedListener {
                NotificationHelper.error("Import failed — please select a .json file")
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
                if (event.value != null) preferencesService.setCurrency(event.value)
            }
        }
        add(currencySelect)
    }
}
