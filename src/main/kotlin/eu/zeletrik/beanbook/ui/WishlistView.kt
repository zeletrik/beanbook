package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService
import java.util.UUID

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
        add(Span("🫘").apply { style["font-size"] = "3rem" })
        add(Span("No wishlist items yet").apply {
            style["font-weight"] = "700"; style["font-size"] = "var(--lumo-font-size-xl)"
        })
        add(Span("Add beans you want to try").apply {
            style["color"] = "var(--lumo-secondary-text-color)"; style["font-size"] = "var(--lumo-font-size-s)"
        })
    }

    // Add form fields
    private val nameField = TextField("Name *").apply { setId("wishlist-name"); width = "100%" }
    private val roasterField = TextField("Roaster").apply { setId("wishlist-roaster"); width = "100%" }
    private val originField = TextField("Origin").apply { setId("wishlist-origin"); width = "100%" }
    private val notesField = TextArea("Notes").apply { setId("wishlist-notes"); width = "100%"; maxHeight = "6rem" }

    init {
        setSizeFull(); isPadding = true; isSpacing = true

        // Add form
        val addForm = VerticalLayout().apply {
            isPadding = true; isSpacing = false; style["gap"] = "0.5rem"
            style["border"] = "1px solid var(--lumo-contrast-10pct)"
            style["border-radius"] = "var(--lumo-border-radius-l)"
            style["background"] = "var(--lumo-base-color)"
            width = "100%"
            add(Span("Add to wishlist").apply {
                style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"
            })
            add(nameField)
            add(roasterField)
            add(originField)
            add(notesField)
            add(Button("Add") {
                val name = nameField.value.trim()
                if (name.isBlank()) {
                    NotificationHelper.error("Name is required")
                } else {
                    wishlistService.upsert(WishlistItem(
                        id = UUID.randomUUID(),
                        name = name,
                        roaster = roasterField.value.trim(),
                        origin = originField.value.trim(),
                        notes = notesField.value.trim().takeIf { it.isNotBlank() },
                    ))
                    nameField.value = ""
                    roasterField.value = ""
                    originField.value = ""
                    notesField.value = ""
                    refreshList()
                }
            }.apply {
                setId("wishlist-add-btn")
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            })
        }

        add(addForm)

        val scrollArea = Div(itemsContainer, emptyState).apply {
            setSizeFull(); style["overflow-y"] = "auto"; style["flex"] = "1"
        }
        add(scrollArea)
        setFlexGrow(1.0, scrollArea)

        refreshList()
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
        val nameSpan = Span(item.name).apply {
            style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
        }
        val secondary = buildString {
            if (item.roaster.isNotBlank()) append(item.roaster)
            if (item.roaster.isNotBlank() && item.origin.isNotBlank()) append("  ·  ")
            if (item.origin.isNotBlank()) append(item.origin)
        }
        val details = VerticalLayout().apply {
            isPadding = false; isSpacing = false; style["gap"] = "0.1rem"
            style["flex"] = "1"; style["overflow"] = "hidden"; style["min-width"] = "0"
            add(nameSpan)
            if (secondary.isNotBlank()) add(Span(secondary).apply {
                style["color"] = "var(--lumo-secondary-text-color)"
                style["font-size"] = "var(--lumo-font-size-s)"
                style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
            })
        }
        val deleteBtn = Button("✕") {
            wishlistService.deleteById(item.id)
            refreshList()
        }.apply {
            addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL)
            element.setAttribute("aria-label", "Delete ${item.name}")
        }
        return HorizontalLayout(details, deleteBtn).apply {
            isSpacing = true; isPadding = true; width = "100%"
            style["align-items"] = "center"
            style["border-radius"] = "var(--lumo-border-radius-l)"
            style["background"] = "var(--lumo-base-color)"
            style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
        }
    }
}
