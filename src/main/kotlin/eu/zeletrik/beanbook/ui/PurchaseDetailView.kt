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
        column.add(buildDetailsSection(purchase))
        column.add(buildStateSection(purchase))
        if (purchase.roastProfile == RoastProfile.OMNI) {
            column.add(buildBrewMethodSection(purchase))
        }
        column.add(buildActionsRow(purchase))
    }

    private fun buildReadingColumn(): VerticalLayout = VerticalLayout().apply {
        setId("detail-column")
        isPadding = false; isSpacing = false
        width = "100%"; style["max-width"] = "640px"
        style["margin"] = "0 auto"
        style["padding"] = "0.75rem 16px 0"
        style["box-sizing"] = "border-box"
        // Cards separate the sections now (no <hr>), so space them evenly.
        style["gap"] = "1rem"
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

    private fun buildDetailsSection(purchase: BeanPurchase): Div {
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
        // Opened/Finished are intentionally omitted here — the Bag tracking timeline owns those dates.
        purchase.notes?.takeUnless { it.isBlank() }?.let {
            grid.add(detailRow("Notes", it).apply { spanFull() })
        }
        purchase.grindSettings?.takeUnless { it.isBlank() }?.let {
            grid.add(detailRow("Grind", it).apply { spanFull() })
        }
        purchase.url.toHref()?.let { href -> grid.add(linkRow(href).apply { spanFull() }) }
        return sectionCard(VaadinIcon.INFO_CIRCLE, "#2e7d9c", "Key facts", null, grid)
    }

    /** Make a grid cell span the full row (both columns on a wide window). */
    private fun HorizontalLayout.spanFull() { style["grid-column"] = "1 / -1" }

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

    private fun buildStateSection(purchase: BeanPurchase): Div {
        val body = VerticalLayout().apply {
            isPadding = false; isSpacing = false; style["gap"] = "0.85rem"
            width = "100%"; style["align-items"] = "stretch"
        }
        body.add(buildTimeline(purchase))
        when (purchase.bagState) {
            BagState.SEALED -> body.add(primaryStateButton("Mark as Opened Today") {
                saveAndRefresh(purchase.copy(openedDate = LocalDate.now()))
            })

            BagState.OPEN -> {
                body.add(primaryStateButton("Mark as Finished Today") {
                    saveAndRefresh(purchase.copy(finishedDate = LocalDate.now()))
                })
                body.add(resetButton { saveAndRefresh(purchase.copy(openedDate = null)) })
            }

            BagState.FINISHED ->
                body.add(resetButton { saveAndRefresh(purchase.copy(openedDate = null, finishedDate = null)) })
        }
        return sectionCard(VaadinIcon.PACKAGE, "#c25e00", "Bag tracking", "From sealed to finished.", body)
    }

    // The bag-state timeline is rendered by the top-level `buildTimeline` / `timelineNode` helpers below
    // (kept out of the class to stay lean and keep each function simple).

    /** Full-width, 44px-tall success action for the state tracker. */
    private fun primaryStateButton(text: String, onClick: () -> Unit): Button =
        Button(text) { onClick() }.apply {
            addThemeVariants(ButtonVariant.LUMO_SUCCESS)
            style["width"] = "100%"; style["min-height"] = "44px"
        }

    private fun resetButton(onClick: () -> Unit): Button =
        Button("Reset to Sealed") { onClick() }.apply {
            addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL)
            style["min-height"] = "44px"; style["align-self"] = "flex-start"
        }

    private fun buildBrewMethodSection(purchase: BeanPurchase): Div {
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
        return sectionCard(VaadinIcon.COFFEE, "var(--lumo-primary-color)", "Brew method", null, usedAsSelect)
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

/**
 * Vertical lifecycle timeline for the bag: Roasted, Purchased, Opened, Finished. Dated events are ordered
 * **chronologically** — roast and purchase dates don't have a fixed order (roast-on-demand roasters ship
 * after you buy, so the purchase can predate the roast) — and any not-yet events (opened/finished) follow
 * as pending nodes. A done milestone is a filled dot + solid connector; pending ones get a hollow dot, a
 * muted label, and a faint connector. The next action lives just below in `buildStateSection`.
 */
private fun buildTimeline(purchase: BeanPurchase): Div {
    val dated = buildList {
        add("Roasted" to purchase.roastDate)
        add("Purchased" to purchase.purchaseDate)
        purchase.openedDate?.let { add("Opened" to it) }
        purchase.finishedDate?.let { add("Finished" to it) }
    }.sortedBy { it.second }
    val pending = buildList {
        if (purchase.openedDate == null) add("Opened")
        if (purchase.finishedDate == null) add("Finished")
    }
    val total = dated.size + pending.size
    return Div().apply {
        setId("detail-timeline"); style["width"] = "100%"
        dated.forEachIndexed { i, (label, date) ->
            // The connector is solid while the next node is also a happened (dated) event.
            add(timelineNode(label, date.toString(), done = true, isLast = i == total - 1, connectorActive = i < dated.lastIndex))
        }
        pending.forEachIndexed { j, label ->
            val sub = if (label == "Opened") "Not opened yet" else "Not finished"
            add(timelineNode(label, sub, done = false, isLast = j == pending.lastIndex, connectorActive = false))
        }
    }
}

/** One milestone row: a dot + (unless last) a connector rail, beside a label and a muted sub-line. */
private fun timelineNode(
    label: String,
    sub: String,
    done: Boolean,
    isLast: Boolean,
    connectorActive: Boolean,
): HorizontalLayout {
    val dot = Div().apply {
        style["width"] = "14px"; style["height"] = "14px"; style["border-radius"] = "50%"
        style["box-sizing"] = "border-box"; style["flex-shrink"] = "0"
        if (done) {
            style["background"] = "var(--lumo-primary-color)"
        } else {
            style["background"] = "var(--lumo-base-color)"; style["border"] = "2px solid var(--lumo-contrast-30pct)"
        }
    }
    val rail = VerticalLayout(dot).apply {
        isPadding = false; isSpacing = false
        width = "16px"; style["min-width"] = "16px"; style["flex-shrink"] = "0"
        style["align-items"] = "center"; style["padding-top"] = "2px"
        if (!isLast) {
            add(Div().apply {
                style["width"] = "2px"; style["flex"] = "1"; style["min-height"] = "1.25rem"; style["margin-top"] = "2px"
                style["background"] = if (connectorActive) "var(--lumo-primary-color)" else "var(--lumo-contrast-20pct)"
            })
        }
    }
    val content = VerticalLayout(
        Span(label).apply {
            style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-s)"
            if (!done) style["color"] = "var(--lumo-secondary-text-color)"
        },
        Span(sub).apply {
            style["font-size"] = "var(--lumo-font-size-xs)"; style["color"] = "var(--lumo-secondary-text-color)"
        },
    ).apply {
        isPadding = false; isSpacing = false; style["gap"] = "0.05rem"
        style["padding-bottom"] = if (isLast) "0" else "0.75rem"
    }
    return HorizontalLayout(rail, content).apply {
        isPadding = false; isSpacing = false; style["gap"] = "0.6rem"
        width = "100%"; style["align-items"] = "stretch"
    }
}
