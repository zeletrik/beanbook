package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
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
        add(buildTopBar())
        val scroll = buildScrollContainer()
        add(scroll)
        setFlexGrow(1.0, scroll)
        scroll.add(buildPhoto(purchase))
        scroll.add(buildHeroSection(purchase))
        scroll.add(Hr())
        scroll.add(buildDetailsSection(purchase))
        scroll.add(Hr())
        scroll.add(buildStateSection(purchase))
        if (purchase.roastProfile == RoastProfile.OMNI) {
            scroll.add(Hr())
            scroll.add(buildBrewMethodSection(purchase))
        }
        scroll.add(Hr())
        scroll.add(buildActionsRow(purchase))
    }

    private fun buildTopBar(): HorizontalLayout {
        val backBtn = Button(Icon(VaadinIcon.ARROW_LEFT)) { onBack() }.apply {
            addThemeVariants(ButtonVariant.LUMO_TERTIARY)
            element.setAttribute("aria-label", "Back")
        }
        return HorizontalLayout(backBtn, Span("Details").apply {
            style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-l)"
        }).apply {
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

    private fun buildPhoto(purchase: BeanPurchase): com.vaadin.flow.component.Component =
        if (purchase.imageData != null) {
            Image(purchase.imageData, "Photo of ${purchase.name}").apply {
                width = "100%"; style["max-height"] = "260px"
                style["object-fit"] = "cover"; style["display"] = "block"
            }
        } else {
            Div(Icon(VaadinIcon.COFFEE).apply {
                setSize("4rem"); style["color"] = "var(--lumo-tertiary-text-color)"
                element.setAttribute("aria-hidden", "true")
            }).apply {
                width = "100%"; style["height"] = "180px"; style["display"] = "flex"
                style["align-items"] = "center"; style["justify-content"] = "center"
                style["background"] = "var(--lumo-contrast-5pct)"
            }
        }

    private fun buildHeroSection(purchase: BeanPurchase): VerticalLayout {
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
        return VerticalLayout().apply {
            isPadding = true; isSpacing = false; style["gap"] = "0.25rem"
            add(H2(purchase.name).apply { style["margin"] = "0" })
            add(Span("${purchase.roaster}  ·  ${purchase.origin}").apply {
                style["color"] = "var(--lumo-secondary-text-color)"
            })
            add(HorizontalLayout(ratingSpan, bagStateBadge(purchase.bagState)).apply {
                isSpacing = true; style["align-items"] = "center"; style["flex-wrap"] = "wrap"
            })
        }
    }

    private fun buildDetailsSection(purchase: BeanPurchase): VerticalLayout = VerticalLayout().apply {
        isPadding = true; isSpacing = false; style["gap"] = "0.4rem"
        add(detailRow("Roast level", purchase.roastLevel.displayName()))
        add(detailRow("Process", purchase.process.displayName()))
        add(detailRow("Roast profile", purchase.roastProfile.displayName()))
        add(detailRow("Price", purchase.price.formatPrice(getCurrency())))
        add(detailRow("Weight", "${purchase.weightGrams} g"))
        add(detailRow("Purchased", purchase.purchaseDate.toString()))
        add(detailRow("Roasted", purchase.roastDate.toString()))
        if (purchase.openedDate != null) add(detailRow("Opened", purchase.openedDate.toString()))
        if (purchase.finishedDate != null) add(detailRow("Finished", purchase.finishedDate.toString()))
        if (purchase.tags.isNotEmpty()) {
            add(detailRow("Tags", purchase.tags.joinToString(", ")).apply { setId("detail-tags-row") })
        }
        if (!purchase.notes.isNullOrBlank()) add(detailRow("Notes", purchase.notes))
        if (!purchase.grindSettings.isNullOrBlank()) add(detailRow("Grind", purchase.grindSettings!!))
        purchase.url.toHref()?.let { href -> add(linkRow(href)) }
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
        isPadding = true; isSpacing = true
        add(Span("State tracker").apply {
            style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"
        })
        when (purchase.bagState) {
            BagState.SEALED -> add(Button("Mark as Opened Today") {
                saveAndRefresh(purchase.copy(openedDate = LocalDate.now()))
            }.apply { addThemeVariants(ButtonVariant.LUMO_SUCCESS) })

            BagState.OPEN -> {
                add(detailRow("Opened on", purchase.openedDate.toString()))
                add(Button("Mark as Finished Today") {
                    saveAndRefresh(purchase.copy(finishedDate = LocalDate.now()))
                }.apply { addThemeVariants(ButtonVariant.LUMO_SUCCESS) })
                add(Button("Reset to Sealed") {
                    saveAndRefresh(purchase.copy(openedDate = null))
                }.apply { addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL) })
            }

            BagState.FINISHED -> {
                add(detailRow("Opened on", purchase.openedDate.toString()))
                add(detailRow("Finished on", purchase.finishedDate.toString()))
                add(Button("Reset to Sealed") {
                    saveAndRefresh(purchase.copy(openedDate = null, finishedDate = null))
                }.apply { addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL) })
            }
        }
    }

    private fun buildBrewMethodSection(purchase: BeanPurchase): VerticalLayout = VerticalLayout().apply {
        isPadding = true; isSpacing = true
        add(Span("Brew method").apply {
            style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"
        })
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
            Button("Edit") { onEdit(purchase) }.apply { addThemeVariants(ButtonVariant.LUMO_PRIMARY) },
            Button("Duplicate") { onDuplicate(purchase) }.apply { addThemeVariants(ButtonVariant.LUMO_CONTRAST) },
            Button("Delete") { onDelete(purchase) }.apply { addThemeVariants(ButtonVariant.LUMO_ERROR) },
        ).apply { isPadding = true; isSpacing = true }

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
