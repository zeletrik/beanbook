package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
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

/**
 * Settings view exposing data export, import, and user preferences such as the display currency.
 *
 * Laid out as the same rounded "cards" used by the Analytics dashboard (see [AnalyticsPanel.section] /
 * its icon circle) so Settings reads as part of the same product. The card + icon-circle styling is
 * duplicated locally to keep the change to one file; a shared `ui/UiComponents.kt` would be a
 * reasonable follow-up to de-duplicate it.
 */
class SettingsView(
    private val exportService: ExportService,
    private val importService: ImportService,
    private val preferencesService: PreferencesService,
    private val onImportComplete: () -> Unit = {},
    private val onCurrencyChanged: () -> Unit = {},
    /** When true (auth enabled), surface the Security section: passkey management and logout. */
    private val securityEnabled: Boolean = false,
    /** Invoked when the user clicks "Log out"; wired to Vaadin's AuthenticationContext.logout(). */
    private val onLogout: () -> Unit = {},
    /** App version shown as a muted chip in the header; null (dev/test) hides the chip. */
    private val appVersion: String? = null,
) : VerticalLayout() {

    init {
        setSizeFull(); isPadding = false; isSpacing = false

        // Pinned header (with the version chip), then the cards scroll below — consistent with the other tabs.
        add(pageHeader("Settings", versionChip()))
        val scroll = Div().apply {
            setSizeFull(); style["overflow-y"] = "auto"; style["flex"] = "1"
            style["padding"] = "1rem"; style["box-sizing"] = "border-box"
            style["display"] = "flex"; style["flex-direction"] = "column"; style["gap"] = "1rem"
            add(buildDataCard(), buildPreferencesCard())
            if (securityEnabled) add(buildSecurityCard())
        }
        add(scroll)
        setFlexGrow(1.0, scroll)
    }

    /** Muted "v{version}" chip shown in the header corner, or null when no build info is available. */
    private fun versionChip(): Span? = appVersion?.let { version ->
        Span("v$version").apply {
            setId("app-version")
            element.setAttribute("title", "App version")
            style["color"] = "var(--lumo-secondary-text-color)"
            style["font-size"] = "var(--lumo-font-size-xs)"; style["font-weight"] = "600"
            style["background"] = "var(--lumo-contrast-5pct)"
            style["border"] = "1px solid var(--lumo-contrast-10pct)"
            style["padding"] = "0.15rem 0.5rem"
            style["border-radius"] = "var(--lumo-border-radius-m)"
            style["white-space"] = "nowrap"
        }
    }

    // ── Data ──────────────────────────────────────────────────────
    private fun buildDataCard(): Div {
        // The supplier runs on a background request thread, so it must not touch the UI. On failure we
        // log and rethrow rather than serving a 0-byte file as a successful download.
        val exportResource = StreamResource("beanbook-export-${LocalDate.now()}.json") {
            try {
                ByteArrayInputStream(exportService.generateJson())
            } catch (e: Exception) {
                log.error("Export failed while generating JSON", e)
                throw e
            }
        }
        val exportAnchor = Anchor(exportResource, "Export Data").apply {
            setId("export-data-btn")
            element.setAttribute("download", true)
            styleAsButton(primary = true)
        }

        val importBuffer = MemoryBuffer()
        val importUpload = Upload(importBuffer).apply {
            setId("import-upload")
            width = "100%"
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

        return sectionCard(
            VaadinIcon.DATABASE, "#2e7d9c",
            "Data", "Back up your collection or restore it from a file.",
            controlWithHelper(exportAnchor, "Download everything as a JSON file."),
            controlWithHelper(importUpload, "Restore from a JSON backup (max 20 MB)."),
        )
    }

    // ── Preferences ───────────────────────────────────────────────
    private fun buildPreferencesCard(): Div {
        val currencyOptions = listOf("€", "$", "£", "¥", "CHF")
        val currencySelect = Select<String>().apply {
            setId("currency-select")
            label = "Currency"
            helperText = "Shown next to every price across the app."
            width = "12rem"
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
        return sectionCard(
            VaadinIcon.COG, "#7c4dff",
            "Preferences", "Personalise how Bean Book looks and behaves.",
            currencySelect,
        )
    }

    // ── Security ──────────────────────────────────────────────────
    // Only meaningful when auth is enabled; the /webauthn/register page is a Spring-rendered page
    // (router-ignore forces a full navigation so Spring's filter serves it, not Vaadin's router).
    private fun buildSecurityCard(): Div {
        val managePasskeys = Anchor("webauthn/register", "Manage passkeys").apply {
            setId("manage-passkeys-link")
            element.setAttribute("router-ignore", true)
            styleAsButton(primary = false)
        }
        val logout = Button("Log out", VaadinIcon.SIGN_OUT.create()) { onLogout() }.apply {
            setId("logout-btn")
            addThemeVariants(ButtonVariant.LUMO_TERTIARY)
            style["min-height"] = "44px"
            style["align-self"] = "flex-start"
        }
        return sectionCard(
            VaadinIcon.LOCK, "#c25e00",
            "Security", "Manage passkeys for this device, or sign out.",
            controlWithHelper(managePasskeys, "Add this device as a passkey, or remove existing ones."),
            logout,
        )
    }

    // Card + icon-circle styling lives in UiComponents.kt (shared with the purchase form).

    /** A control with a small muted helper line beneath it. */
    private fun controlWithHelper(control: Component, helper: String): Div =
        Div(control, Span(helper).apply {
            style["color"] = "var(--lumo-secondary-text-color)"; style["font-size"] = "var(--lumo-font-size-xs)"
        }).apply {
            style["display"] = "flex"; style["flex-direction"] = "column"
            style["gap"] = "0.3rem"; style["align-items"] = "flex-start"; style["width"] = "100%"
        }

    /** Styles an [Anchor] to read as a 44px Lumo button (primary fill or subtle secondary). */
    private fun Anchor.styleAsButton(primary: Boolean) {
        style["display"] = "inline-flex"; style["align-items"] = "center"; style["justify-content"] = "center"
        style["gap"] = "0.5rem"; style["min-height"] = "44px"; style["padding"] = "0 1.25rem"
        style["border-radius"] = "var(--lumo-border-radius-m)"; style["text-decoration"] = "none"
        style["font-weight"] = "600"; style["box-sizing"] = "border-box"
        if (primary) {
            style["background"] = "var(--lumo-primary-color)"; style["color"] = "var(--lumo-primary-contrast-color)"
        } else {
            style["background"] = "var(--lumo-contrast-5pct)"; style["color"] = "var(--lumo-body-text-color)"
            style["border"] = "1px solid var(--lumo-contrast-10pct)"
        }
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
