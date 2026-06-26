package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import eu.zeletrik.beanbook.beans.BagState
import eu.zeletrik.beanbook.beans.BeanPurchase

/** Renders a single purchase as a tappable list row. [onOpen] fires when the row is clicked. */
internal fun beanCard(purchase: BeanPurchase, currency: String, onOpen: () -> Unit): HorizontalLayout {
    val thumbnail = beanCardThumbnail(purchase)
    val details = beanCardDetails(purchase, currency)
    return HorizontalLayout(thumbnail, details).apply {
        addClassName("bean-row")
        isSpacing = false; isPadding = true
        style["align-items"] = "center"; style["gap"] = "0.75rem"
        style["border-radius"] = "var(--lumo-border-radius-l)"
        style["background"] = "var(--lumo-base-color)"
        style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
        style["border"] = "1px solid var(--lumo-contrast-10pct)"
        style["cursor"] = "pointer"; style["width"] = "100%"
        addClickListener { onOpen() }
    }
}

private fun beanCardThumbnail(purchase: BeanPurchase): Div =
    if (purchase.imageData != null) {
        val imgWrapper = Div().apply {
            style["width"] = "72px"; style["min-width"] = "72px"; style["height"] = "72px"
            style["border-radius"] = "var(--lumo-border-radius-m)"; style["overflow"] = "hidden"
            style["flex-shrink"] = "0"
        }
        imgWrapper.add(Image(purchase.imageData, "Photo of ${purchase.name}").apply {
            style["width"] = "72px"; style["height"] = "72px"; style["object-fit"] = "cover"
            style["display"] = "block"
        })
        imgWrapper
    } else {
        Div(Icon(VaadinIcon.COFFEE).apply {
            setSize("2rem"); style["color"] = "var(--lumo-tertiary-text-color)"
            element.setAttribute("aria-hidden", "true")
        }).apply {
            style["width"] = "72px"; style["min-width"] = "72px"; style["height"] = "72px"
            style["border-radius"] = "var(--lumo-border-radius-m)"
            style["background"] = "var(--lumo-contrast-5pct)"
            style["display"] = "flex"; style["align-items"] = "center"
            style["justify-content"] = "center"
            style["flex-shrink"] = "0"
        }
    }

private fun beanCardDetails(purchase: BeanPurchase, currency: String): VerticalLayout {
    val stateBadge = bagStateBadge(purchase.bagState, small = true)
    val bottomRow = HorizontalLayout().apply {
        isSpacing = true; isPadding = false
        style["align-items"] = "center"; style["flex-wrap"] = "wrap"; style["gap"] = "0.3rem"
    }
    val ratingText = purchase.rating.toStars()
    if (ratingText.isNotEmpty()) {
        bottomRow.add(Span(ratingText).apply {
            style["font-size"] = "0.85rem"; style["letter-spacing"] = "0.04rem"
        })
    }
    bottomRow.add(stateBadge)
    return VerticalLayout().apply {
        isPadding = false; isSpacing = false
        style["gap"] = "0.15rem"; style["flex"] = "1"; style["overflow"] = "hidden"
        style["min-width"] = "0"
        add(Span(purchase.name).apply {
            style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
        })
        add(Span("${purchase.roaster}  ·  ${purchase.originLabel()}").apply {
            style["color"] = "var(--lumo-secondary-text-color)"; style["font-size"] = "var(--lumo-font-size-s)"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
        })
        add(Span("${purchase.price.formatPrice(currency)}  ·  ${purchase.weightGrams} g").apply {
            style["font-size"] = "var(--lumo-font-size-s)"; style["color"] = "var(--lumo-secondary-text-color)"
        })
        add(bottomRow)
    }
}
