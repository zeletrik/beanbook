package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger(WishlistView::class.java)

/** Mobile-friendly view for browsing, adding, and removing wishlist beans backed by [WishlistService]. */
class WishlistView(private val wishlistService: WishlistService) : VerticalLayout() {

    private val itemsContainer = VerticalLayout().apply {
        isPadding = false; isSpacing = false; style["gap"] = "0.5rem"; width = "100%"
    }

    private val emptyState = Div().apply {
        setId("wishlist-empty-state")
        style["flex"] = "1"
        style["width"] = "100%"
        style["display"] = "flex"
        style["flex-direction"] = "column"
        style["align-items"] = "center"
        style["justify-content"] = "center"
        style["gap"] = "0.75rem"
        style["padding"] = "2rem 1rem"
        style["text-align"] = "center"
        style["box-sizing"] = "border-box"
        add(Icon(VaadinIcon.COFFEE).apply {
            setSize("3rem"); style["color"] = "var(--lumo-tertiary-text-color)"
            element.setAttribute("aria-hidden", "true")
        })
        add(Span("No wishlist items yet").apply {
            style["font-weight"] = "700"; style["font-size"] = "var(--lumo-font-size-xl)"
        })
        add(Span("Add beans you want to try").apply {
            style["color"] = "var(--lumo-secondary-text-color)"; style["font-size"] = "var(--lumo-font-size-s)"
        })
        add(Button("Add your first wishlist bean") { openAddDialog() }.apply {
            setId("wishlist-empty-cta")
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            style["margin-top"] = "0.5rem"
        })
    }

    // Add form fields (live inside the add dialog, opened on demand).
    private val nameField = TextField("Name *").apply { setId("wishlist-name"); width = "100%" }
    private val roasterField = TextField("Roaster").apply { setId("wishlist-roaster"); width = "100%" }
    private val originField = TextField("Origin").apply { setId("wishlist-origin"); width = "100%" }
    private val notesField = TextArea("Notes").apply { setId("wishlist-notes"); width = "100%"; maxHeight = "6rem" }
    private val urlField = TextField("Link").apply {
        setId("wishlist-url"); width = "100%"
        placeholder = "Product or roaster page"
        isClearButtonVisible = true
    }

    /** Built once and reused; opening focuses entry without the list losing its place. */
    private val addDialog: Dialog by lazy { buildAddDialog() }

    init {
        setSizeFull(); isPadding = true; isSpacing = false; style["gap"] = "0.75rem"

        val header = HorizontalLayout(
            H2("Wishlist").apply { style["margin"] = "0" },
            Button("Add", Icon(VaadinIcon.PLUS)) { openAddDialog() }.apply {
                setId("wishlist-open-add-btn")
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            },
        ).apply {
            width = "100%"; isPadding = false; isSpacing = false
            style["align-items"] = "center"; style["justify-content"] = "space-between"
        }
        add(header)

        val scrollArea = Div(itemsContainer, emptyState).apply {
            setSizeFull(); style["overflow-y"] = "auto"; style["flex"] = "1"
        }
        add(scrollArea)
        setFlexGrow(1.0, scrollArea)

        refreshList()
    }

    private fun buildAddDialog(): Dialog = Dialog().apply {
        setId("wishlist-add-dialog")
        headerTitle = "Add to wishlist"
        width = "min(92vw, 26rem)"
        isCloseOnEsc = true
        add(VerticalLayout(nameField, roasterField, originField, urlField, notesField).apply {
            isPadding = false; isSpacing = false; style["gap"] = "0.5rem"; width = "100%"
        })
        footer.add(
            Button("Cancel") { close() }.apply { setId("wishlist-cancel-add-btn") },
            Button("Add") { addItem() }.apply {
                setId("wishlist-add-btn")
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            },
        )
    }

    /** Opens the add form as a focused modal, with all fields reset. */
    internal fun openAddDialog(): Dialog {
        nameField.value = ""
        roasterField.value = ""
        originField.value = ""
        notesField.value = ""
        urlField.value = ""
        addDialog.open()
        return addDialog
    }

    private fun addItem() {
        val name = nameField.value.trim()
        if (name.isBlank()) {
            NotificationHelper.error("Name is required")
            return
        }
        try {
            wishlistService.upsert(WishlistItem(
                id = UUID.randomUUID(),
                name = name,
                roaster = roasterField.value.trim(),
                origin = originField.value.trim(),
                notes = notesField.value.trim().takeIf { it.isNotBlank() },
                url = urlField.value.toHref(),
            ))
            addDialog.close()
            refreshList()
        } catch (e: Exception) {
            log.error("Failed to add wishlist item '{}'", name, e)
            NotificationHelper.error("Failed to add — please try again")
        }
    }

    internal fun refreshList() {
        val items = wishlistService.findAll()
        itemsContainer.removeAll()
        if (items.isEmpty()) {
            itemsContainer.isVisible = false
            emptyState.isVisible = true
        } else {
            emptyState.isVisible = false
            itemsContainer.isVisible = true
            items.forEach { item -> itemsContainer.add(buildItemRow(item)) }
        }
    }

    private fun buildItemRow(item: WishlistItem): HorizontalLayout {
        val thumbnail = Div(Icon(VaadinIcon.COFFEE).apply {
            setSize("2rem"); style["color"] = "var(--lumo-tertiary-text-color)"
            element.setAttribute("aria-hidden", "true")
        }).apply {
            style["width"] = "72px"; style["min-width"] = "72px"; style["height"] = "72px"
            style["border-radius"] = "var(--lumo-border-radius-m)"
            style["background"] = "var(--lumo-contrast-5pct)"
            style["display"] = "flex"; style["align-items"] = "center"; style["justify-content"] = "center"
            style["flex-shrink"] = "0"
        }

        val details = VerticalLayout().apply {
            setId("wishlist-open-${item.id}")
            isPadding = false; isSpacing = false; style["gap"] = "0.15rem"
            style["flex"] = "1"; style["overflow"] = "hidden"; style["min-width"] = "0"
            style["align-items"] = "stretch"; style["cursor"] = "pointer"
            add(Span(item.name).apply {
                style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"
                style["display"] = "block"; style["min-width"] = "0"
                style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
            })
            if (item.roaster.isNotBlank()) add(metaLine(VaadinIcon.SHOP, item.roaster))
            if (item.origin.isNotBlank()) add(metaLine(VaadinIcon.MAP_MARKER, item.origin))
            if (!item.notes.isNullOrBlank()) add(notesPreview(item.notes))
            item.url.toHref()?.let { add(linkChip(it)) }
            addClickListener { openDetail(item) }
        }

        val chevron = Icon(VaadinIcon.ANGLE_RIGHT).apply {
            setSize("1.5rem"); style["color"] = "var(--lumo-tertiary-text-color)"
            style["flex-shrink"] = "0"; element.setAttribute("aria-hidden", "true")
        }
        val deleteBtn = Button(Icon(VaadinIcon.TRASH)) { confirmDelete(item) }.apply {
            setId("wishlist-delete-${item.id}")
            addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL)
            element.setAttribute("aria-label", "Delete ${item.name}")
            style["flex-shrink"] = "0"
        }

        return HorizontalLayout(thumbnail, details, chevron, deleteBtn).apply {
            isSpacing = false; isPadding = true; width = "100%"
            style["gap"] = "0.75rem"; style["align-items"] = "center"
            style["border-radius"] = "var(--lumo-border-radius-l)"
            style["background"] = "var(--lumo-base-color)"
            style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
            style["border"] = "1px solid var(--lumo-contrast-10pct)"
        }
    }

    /** Icon + ellipsized text line for roaster/origin — mirrors the Beans list rows. */
    private fun metaLine(icon: VaadinIcon, text: String): HorizontalLayout =
        HorizontalLayout(
            Icon(icon).apply {
                setSize("var(--lumo-font-size-s)"); style["color"] = "var(--lumo-secondary-text-color)"
                style["flex"] = "0 0 auto"; element.setAttribute("aria-hidden", "true")
            },
            Span(text).apply {
                style["color"] = "var(--lumo-secondary-text-color)"; style["font-size"] = "var(--lumo-font-size-s)"
                style["flex"] = "1"; style["min-width"] = "0"
                style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
            },
        ).apply {
            isPadding = false; isSpacing = false; style["gap"] = "0.4rem"
            style["align-items"] = "center"; width = "100%"; style["min-width"] = "0"; style["overflow"] = "hidden"
        }

    private fun notesPreview(notes: String): Span = Span(notes).apply {
        style["color"] = "var(--lumo-secondary-text-color)"
        style["font-size"] = "var(--lumo-font-size-xs)"
        style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
    }

    /** Non-interactive link indicator on the card (the clickable anchor lives in the detail dialog). */
    private fun linkChip(href: String): HorizontalLayout = HorizontalLayout(
        Icon(VaadinIcon.EXTERNAL_LINK).apply {
            setSize("0.8rem"); style["color"] = "var(--lumo-primary-color)"
            element.setAttribute("aria-hidden", "true")
        },
        Span(href.toDisplayLink()).apply {
            style["color"] = "var(--lumo-primary-color)"; style["font-size"] = "var(--lumo-font-size-xs)"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
        },
    ).apply {
        isPadding = false; isSpacing = false; style["gap"] = "0.25rem"
        style["align-items"] = "center"; style["min-width"] = "0"
    }

    /** Opens the full detail of a wishlist item (all saved fields + a clickable link). */
    internal fun openDetail(item: WishlistItem): Dialog {
        val dialog = Dialog().apply { setId("wishlist-detail-dialog"); width = "min(90vw, 24rem)" }
        val body = VerticalLayout().apply {
            isPadding = false; isSpacing = false; style["gap"] = "0.5rem"; width = "100%"
            add(H2(item.name).apply { style["margin"] = "0"; style["font-size"] = "var(--lumo-font-size-xl)" })
            secondaryLine(item)?.let { add(it) }
            if (!item.notes.isNullOrBlank()) {
                add(Span("Notes").apply {
                    style["color"] = "var(--lumo-secondary-text-color)"; style["font-size"] = "var(--lumo-font-size-xs)"
                    style["margin-top"] = "0.25rem"
                })
                add(Paragraph(item.notes).apply { style["margin"] = "0" })
            }
            item.url.toHref()?.let { href ->
                add(Anchor(href, href.toDisplayLink()).apply {
                    setId("wishlist-detail-link")
                    setTarget("_blank")
                    element.setAttribute("rel", "noopener noreferrer")
                    style["color"] = "var(--lumo-primary-color)"; style["word-break"] = "break-all"
                    style["margin-top"] = "0.25rem"
                })
            }
        }
        val actions = HorizontalLayout(
            Button("Delete") {
                dialog.close()
                confirmDelete(item)
            }.apply { setId("wishlist-detail-delete"); addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY) },
            Button("Close") { dialog.close() }.apply { setId("wishlist-detail-close") },
        ).apply { isSpacing = true; isPadding = false; style["margin-top"] = "0.5rem" }
        dialog.add(VerticalLayout(body, actions).apply { isPadding = false; isSpacing = false; width = "100%" })
        dialog.open()
        return dialog
    }

    /** Compact "roaster · origin" subtitle used inside the detail dialog. */
    private fun secondaryLine(item: WishlistItem): Span? {
        val text = buildString {
            if (item.roaster.isNotBlank()) append(item.roaster)
            if (item.roaster.isNotBlank() && item.origin.isNotBlank()) append("  ·  ")
            if (item.origin.isNotBlank()) append(item.origin)
        }
        if (text.isBlank()) return null
        return Span(text).apply {
            style["color"] = "var(--lumo-secondary-text-color)"
            style["font-size"] = "var(--lumo-font-size-s)"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
        }
    }

    private fun confirmDelete(item: WishlistItem) {
        val dialog = Dialog()
        dialog.setId("wishlist-delete-confirm")
        dialog.add(Paragraph("Remove '${item.name}' from your wishlist?"))
        val confirm = Button("Remove") {
            try {
                wishlistService.deleteById(item.id)
                NotificationHelper.successWithUndo("Removed '${item.name}'") {
                    wishlistService.upsert(item)
                    refreshList()
                }
                refreshList()
            } catch (e: Exception) {
                log.error("Failed to delete wishlist item {}", item.id, e)
                NotificationHelper.error("Failed to delete — please try again")
            }
            dialog.close()
        }.apply { setId("wishlist-confirm-delete-btn"); addThemeVariants(ButtonVariant.LUMO_ERROR) }
        val cancel = Button("Cancel") { dialog.close() }.apply { setId("wishlist-cancel-delete-btn") }
        dialog.add(HorizontalLayout(confirm, cancel))
        dialog.open()
    }
}
