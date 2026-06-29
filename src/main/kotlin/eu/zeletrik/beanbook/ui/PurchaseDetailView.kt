package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.badge.Badge
import com.vaadin.flow.component.badge.BadgeVariant
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.select.Select
import eu.zeletrik.beanbook.beans.BagState
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BrewTarget
import eu.zeletrik.beanbook.beans.RoastProfile
import java.time.LocalDate

/** Width of the compact bag thumbnail shown beside the hero identity. */
private const val PHOTO_THUMB_WIDTH = "120px"

class PurchaseDetailView(
    private val onBack: () -> Unit,
    private val onEdit: (BeanPurchase) -> Unit,
    private val onDelete: (BeanPurchase) -> Unit,
    private val onSave: (BeanPurchase) -> Unit,
    private val onDuplicate: (BeanPurchase) -> Unit = {},
    private val getCurrency: () -> String = { "€" },
) : VerticalLayout() {

    private var current: BeanPurchase? = null

    init {
        setSizeFull()
        isPadding = false
        isSpacing = false
        isVisible = false
    }

    fun show(purchase: BeanPurchase) {
        current = purchase
        isVisible = true
        removeAll()
        add(buildTopBar(purchase))
        val scroll = buildScrollContainer()
        add(scroll)
        setFlexGrow(1.0, scroll)
        // Centered, max-width reading column so content never stretches edge-to-edge on wide windows
        // (the top bar stays outside it, so its divider still spans the full screen).
        val column = buildReadingColumn()
        scroll.add(column)
        column.add(buildHeroSection(purchase))
        column.add(Hr())
        column.add(buildDetailsSection(purchase))
        column.add(Hr())
        column.add(buildStateSection(purchase))
        if (purchase.roastProfile == RoastProfile.OMNI) {
            column.add(Hr())
            column.add(buildBrewMethodSection(purchase))
        }
        column.add(Hr())
        column.add(buildActionsRow(purchase))
    }

    private fun buildReadingColumn(): VerticalLayout = VerticalLayout().apply {
        setId("detail-column")
        isPadding = false; isSpacing = false
        width = "100%"; style["max-width"] = "640px"
        style["margin"] = "0 auto"
        style["padding"] = "0.75rem 16px 0"
        style["box-sizing"] = "border-box"
    }

    private fun buildTopBar(purchase: BeanPurchase): HorizontalLayout {
        val backBtn = Button(Icon(VaadinIcon.ARROW_LEFT)) { onBack() }.apply {
            addThemeVariants(ButtonVariant.LUMO_TERTIARY)
            element.setAttribute("aria-label", "Back")
            style["flex"] = "0 0 auto"
        }
        // The bean's identity stays visible in the (non-scrolling) header. Single line with ellipsis so a
        // long "{name} by {roaster}" never breaks the bar; the full text is in the tooltip.
        val titleText = "${purchase.name} by ${purchase.roaster}"
        val title = Span(titleText).apply {
            setId("detail-title")
            style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"
            style["flex"] = "1"; style["min-width"] = "0"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
            element.setAttribute("title", titleText)
        }
        return HorizontalLayout(backBtn, title).apply {
            isSpacing = true; isPadding = true
            style["align-items"] = "center"
            style["border-bottom"] = "1px solid var(--lumo-contrast-10pct)"
            width = "100%"
        }
    }

    private fun buildScrollContainer(): VerticalLayout = VerticalLayout().apply {
        setSizeFull(); isPadding = false; isSpacing = false
        style["overflow-y"] = "auto"; style["padding-bottom"] = "1rem"
    }

    private fun buildPhoto(purchase: BeanPurchase): Component {
        val data = purchase.imageData
            ?: return Div(Icon(VaadinIcon.COFFEE).apply {
                setSize("2.5rem"); style["color"] = "var(--lumo-tertiary-text-color)"
                element.setAttribute("aria-hidden", "true")
            }).apply {
                // No photo: a square placeholder matching the thumbnail width.
                setId("detail-photo-card")
                style["flex"] = "0 0 auto"
                style["width"] = PHOTO_THUMB_WIDTH; style["height"] = PHOTO_THUMB_WIDTH
                style["display"] = "flex"; style["align-items"] = "center"; style["justify-content"] = "center"
                style["background"] = "var(--lumo-contrast-5pct)"
                style["border"] = "1px solid var(--lumo-contrast-10pct)"
                style["border-radius"] = "var(--lumo-border-radius-l)"
            }

        // Compact thumbnail beside the hero. Fixed width, natural height (width:100% / height:auto), so
        // the frame still takes the photo's own aspect ratio — no crop (#17), no letterbox. The whole
        // card doesn't grow/shrink in the row (flex 0 0 auto); tap opens the full-size dialog.
        val photo = Image(data, "Photo of ${purchase.name}").apply {
            setId("detail-photo")
            style["display"] = "block"
            style["width"] = "100%"; style["height"] = "auto"
            style["cursor"] = "pointer"
            element.setAttribute("title", "Tap to enlarge")
            addClickListener { openPhotoDialog(data, purchase.name) }
        }
        val border =  Div(photo).apply {
            setId("detail-photo-card-border")
            style["flex"] = "0 0 auto"
            style["width"] = PHOTO_THUMB_WIDTH
            style["overflow"] = "hidden"
            style["line-height"] = "0" // no descender gap under the block image
            style["border"] = "1px solid var(--lumo-contrast-10pct)"
            style["border-radius"] = "var(--lumo-border-radius-l)"
        }
        return Div(border).apply {
            setId("detail-photo-card")
            style["flex"] = "0 0 auto"
            style["width"] = PHOTO_THUMB_WIDTH
            style["overflow"] = "hidden"
            style["line-height"] = "0" // no descender gap under the block image
            style["height"] = "100%"
            style["align-content"] = "center"
        }
    }

    /** Opens the bag photo full-size in a dismissible dialog. */
    private fun openPhotoDialog(data: ByteArray, name: String) {
        val full = Image(data, "Photo of $name").apply {
            setId("detail-photo-full")
            style["max-width"] = "80vw"; style["max-height"] = "90vh"
            style["object-fit"] = "contain"; style["display"] = "block"
        }
        Dialog(full).apply {
            setId("detail-photo-dialog")
            headerTitle = name
            isCloseOnEsc = true
            isCloseOnOutsideClick = true
            open()
        }
    }

    private fun buildHeroSection(purchase: BeanPurchase): HorizontalLayout {
        val ratingSpan = if (purchase.rating != null) {
            Span(purchase.rating.toStars()).apply {
                style["font-size"] = "1.2rem"; style["letter-spacing"] = "0.1rem"
            }
        } else {
            Span("Not rated").apply {
                style["color"] = "var(--lumo-secondary-text-color)"
                style["font-size"] = "var(--lumo-font-size-s)"
            }
        }
        val identity = VerticalLayout().apply {
            isPadding = false; isSpacing = false; style["gap"] = "0.25rem"
            style["min-width"] = "0" // let long names wrap/shrink instead of overflowing the row
            add(H2(purchase.name).apply { style["margin"] = "0"; style["overflow-wrap"] = "anywhere" })
            add(metaLine(VaadinIcon.SHOP, purchase.roaster))
            add(metaLine(VaadinIcon.MAP_MARKER, purchase.originLabel()))
            purchase.tags.takeIf { it.isNotEmpty() }?.let {
                add(metaLine(VaadinIcon.TAGS, it.joinToString(", ")).apply { setId("detail-tags-row") })
            }
            add(HorizontalLayout(ratingSpan, bagStateBadge(purchase.bagState)).apply {
                isSpacing = true; style["align-items"] = "center"; style["flex-wrap"] = "wrap"
                style["margin-top"] = "0.15rem"
            })
        }
        // Compact thumbnail to the left, identity fills the rest; top-aligned so the photo sits with the name.
        return HorizontalLayout(identity, buildPhoto(purchase)).apply {
            isPadding = false; isSpacing = true
            style["padding"] = "var(--lumo-space-m) 0"
            style["align-items"] = "flex-start"
            width = "100%"
            setFlexGrow(1.0, identity)
        }
    }

    /** A secondary-text line with a small leading icon — used for the roaster and origin in the hero. */
    private fun metaLine(icon: VaadinIcon, text: String): HorizontalLayout =
        HorizontalLayout(
            Icon(icon).apply {
                setSize("1rem")
                style["color"] = "var(--lumo-secondary-text-color)"
                style["flex"] = "0 0 auto"
                element.setAttribute("aria-hidden", "true")
            },
            Span(text).apply {
                style["color"] = "var(--lumo-secondary-text-color)"
                style["overflow-wrap"] = "anywhere"
            },
        ).apply {
            isPadding = false; isSpacing = false
            style["gap"] = "0.4rem"
            style["align-items"] = "center"
        }

    private fun buildDetailsSection(purchase: BeanPurchase): VerticalLayout = VerticalLayout().apply {
        isPadding = false; isSpacing = false
        style["padding"] = "var(--lumo-space-m) 0"; style["gap"] = "0.6rem"
        add(eyebrow("Key facts"))
        // Auto-fit grid: one column on a phone, two on a wide window — no media query needed.
        val grid = Div().apply {
            style["display"] = "grid"
            style["grid-template-columns"] = "repeat(auto-fit, minmax(220px, 1fr))"
            style["column-gap"] = "1rem"; style["row-gap"] = "0.4rem"
            style["width"] = "100%"
        }
        grid.add(detailRow("Roast level", purchase.roastLevel.displayName()))
        grid.add(detailRow("Process", purchase.process.displayName()))
        grid.add(detailRow("Roast profile", purchase.roastProfile.displayName()))
        grid.add(detailRow("Price", purchase.price.formatPrice(getCurrency())))
        grid.add(detailRow("Weight", "${purchase.weightGrams} g"))
        grid.add(detailRow("Purchased", purchase.purchaseDate.toString()))
        grid.add(detailRow("Roasted", purchase.roastDate.toString()))
        if (purchase.openedDate != null) grid.add(detailRow("Opened", purchase.openedDate.toString()))
        if (purchase.finishedDate != null) grid.add(detailRow("Finished", purchase.finishedDate.toString()))
        purchase.notes?.takeUnless { it.isBlank() }?.let {
            grid.add(detailRow("Notes", it).apply { spanFull() })
        }
        purchase.grindSettings?.takeUnless { it.isBlank() }?.let {
            grid.add(detailRow("Grind", it).apply { spanFull() })
        }
        purchase.url.toHref()?.let { href -> grid.add(linkRow(href).apply { spanFull() }) }
        add(grid)
    }

    /** Make a grid cell span the full row (both columns on a wide window). */
    private fun HorizontalLayout.spanFull() { style["grid-column"] = "1 / -1" }

    /** Small uppercase section label — hierarchy via size/spacing/contrast, not a new colour. */
    private fun eyebrow(text: String): Span = Span(text).apply {
        style["font-size"] = "var(--lumo-font-size-xs)"
        style["letter-spacing"] = "0.08em"
        style["text-transform"] = "uppercase"
        style["color"] = "var(--lumo-secondary-text-color)"
        style["font-weight"] = "600"
    }

    private fun linkRow(href: String): HorizontalLayout =
        HorizontalLayout(
            Span("Link").apply {
                style["color"] = "var(--lumo-secondary-text-color)"
                style["min-width"] = "100px"; style["font-size"] = "var(--lumo-font-size-s)"
            },
            Anchor(href, href.toDisplayLink()).apply {
                setId("detail-link")
                setTarget("_blank")
                element.setAttribute("rel", "noopener noreferrer")
                style["font-size"] = "var(--lumo-font-size-s)"; style["flex"] = "1"
                style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
            },
        ).apply { isSpacing = true; width = "100%"; style["align-items"] = "center" }

    private fun buildStateSection(purchase: BeanPurchase): VerticalLayout = VerticalLayout().apply {
        isPadding = false; isSpacing = true
        style["padding"] = "var(--lumo-space-m) 0"
        add(eyebrow("State tracker"))
        when (purchase.bagState) {
            BagState.SEALED -> add(primaryStateButton("Mark as Opened Today") {
                saveAndRefresh(purchase.copy(openedDate = LocalDate.now()))
            })

            BagState.OPEN -> {
                add(detailRow("Opened on", purchase.openedDate.toString()))
                add(primaryStateButton("Mark as Finished Today") {
                    saveAndRefresh(purchase.copy(finishedDate = LocalDate.now()))
                })
                add(resetButton { saveAndRefresh(purchase.copy(openedDate = null)) })
            }

            BagState.FINISHED -> {
                add(detailRow("Opened on", purchase.openedDate.toString()))
                add(detailRow("Finished on", purchase.finishedDate.toString()))
                add(resetButton { saveAndRefresh(purchase.copy(openedDate = null, finishedDate = null)) })
            }
        }
    }

    /** Full-width, 44px-tall success action for the state tracker. */
    private fun primaryStateButton(text: String, onClick: () -> Unit): Button =
        Button(text) { onClick() }.apply {
            addThemeVariants(ButtonVariant.LUMO_SUCCESS)
            style["width"] = "100%"; style["min-height"] = "44px"
        }

    private fun resetButton(onClick: () -> Unit): Button =
        Button("Reset to Sealed") { onClick() }.apply {
            addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL)
            style["min-height"] = "44px"
        }

    private fun buildBrewMethodSection(purchase: BeanPurchase): VerticalLayout = VerticalLayout().apply {
        isPadding = false; isSpacing = true
        style["padding"] = "var(--lumo-space-m) 0"
        add(eyebrow("Brew method"))
        val usedAsSelect = Select<BrewTarget?>().apply {
            setId("used-as-select")
            label = "Used as"
            setItemLabelGenerator { rp -> rp?.displayName() ?: "Not set" }
            setItems(null, BrewTarget.ESPRESSO, BrewTarget.FILTER)
            value = purchase.usedAs
            addValueChangeListener { event ->
                if (event.value == purchase.usedAs) return@addValueChangeListener
                saveAndRefresh(purchase.copy(usedAs = event.value))
            }
        }
        add(usedAsSelect)
    }

    private fun buildActionsRow(purchase: BeanPurchase): HorizontalLayout =
        HorizontalLayout(
            actionButton("Edit", ButtonVariant.LUMO_PRIMARY) { onEdit(purchase) },
            actionButton("Duplicate", ButtonVariant.LUMO_CONTRAST) { onDuplicate(purchase) },
            actionButton("Delete", ButtonVariant.LUMO_ERROR) { onDelete(purchase) },
        ).apply {
            isPadding = false; isSpacing = true
            style["padding"] = "var(--lumo-space-m) 0 var(--lumo-space-l)"
            style["flex-wrap"] = "wrap"
        }

    private fun actionButton(text: String, variant: ButtonVariant, onClick: () -> Unit): Button =
        Button(text) { onClick() }.apply {
            addThemeVariants(variant)
            style["min-height"] = "44px"
        }

    private fun saveAndRefresh(updated: BeanPurchase) {
        onSave(updated)
        show(updated)
    }

    private fun detailRow(label: String, value: String): HorizontalLayout =
        HorizontalLayout(
            Span(label).apply {
                style["color"] = "var(--lumo-secondary-text-color)"
                style["min-width"] = "100px"; style["font-size"] = "var(--lumo-font-size-s)"
            },
            Span(value).apply { style["font-size"] = "var(--lumo-font-size-s)"; style["flex"] = "1" }
        ).apply { isSpacing = true; width = "100%" }
}
