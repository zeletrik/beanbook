package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import eu.zeletrik.beanbook.beans.BeanPurchase

/** Renders a single purchase as a tappable list row. [onOpen] fires when the row is clicked. */
internal fun beanCard(purchase: BeanPurchase, currency: String, onOpen: () -> Unit): HorizontalLayout {
    val thumbnail = beanCardThumbnail(purchase)
    val details = beanCardDetails(purchase, currency)

    val chevron = Icon(VaadinIcon.ANGLE_RIGHT).apply {
        setSize("1.5rem"); style["color"] = "var(--lumo-tertiary-text-color)"
        style["flex-shrink"] = "0"
        element.setAttribute("aria-hidden", "true")
    }

    val rightRow = HorizontalLayout().apply {
        isPadding = false; isSpacing = false
        style["gap"] = "0.75rem"
        width = "auto"; style["flex-shrink"] = "0"; style["align-self"] = "stretch"
        style["align-items"] = "flex-start"; style["justify-content"] = "space-between"
        add(bagStateBadge(purchase.bagState, small = true))
        purchase.url.toHref()?.let { href ->
            add(
                Span(beanCardLinkIcon(href)).apply {
                    style["flex-shrink"] = "0"
                }
            )
        }
    }
    val rightCol = VerticalLayout().apply {
        isPadding = false; isSpacing = false
        width = "auto"; style["flex-shrink"] = "0"; style["align-self"] = "stretch"
        style["align-items"] = "flex-end"; style["justify-content"] = "space-between"
        add(rightRow)
        add(chevron)
        add(Span( purchase.rating.toStars()).apply {
            style["font-size"] = "0.85rem"; style["letter-spacing"] = "0.04rem"
        })
    }
    return HorizontalLayout(thumbnail, details, rightCol).apply {
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

/**
 *
 *         HorizontalLayout(
 *             Icon(icon).apply {
 *                 setSize("1rem")
 *                 style["color"] = "var(--lumo-secondary-text-color)"
 *                 style["flex"] = "0 0 auto"
 *                 element.setAttribute("aria-hidden", "true")
 *             },
 *             Span(text).apply {
 *                 style["color"] = "var(--lumo-secondary-text-color)"
 *                 style["overflow-wrap"] = "anywhere"
 *             },
 *         ).apply {
 *             isPadding = false; isSpacing = false
 *             style["gap"] = "0.4rem"
 *             style["align-items"] = "center"
 *         }
 */

private fun beanCardDetails(purchase: BeanPurchase, currency: String): VerticalLayout {
    return VerticalLayout().apply {
        isPadding = false; isSpacing = false
        style["gap"] = "0.15rem"; style["flex"] = "1"; style["overflow"] = "hidden"
        // min-width:0 lets this column shrink below its content so text truncates instead of pushing into
        // the right side; align-items:stretch makes the children fill the column so ellipsis has a bound.
        style["min-width"] = "0"; style["align-items"] = "stretch"
        add(Span(purchase.name).apply {
            style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"
            style["display"] = "block"; style["min-width"] = "0"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
        })
        add(metaLine(VaadinIcon.SHOP, purchase.roaster))
        add(metaLine(VaadinIcon.MAP_MARKER, purchase.originLabel()))
        add(Span("${purchase.price.formatPrice(currency)}  ·  ${purchase.weightGrams} g").apply {
            style["font-size"] = "var(--lumo-font-size-s)"; style["color"] = "var(--lumo-secondary-text-color)"
            style["display"] = "block"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
        })
    }
}

/**
 * Creates a horizontal layout containing an icon and text styled as metadata information.
 * The layout is configured with secondary text color, small font size, and automatic text overflow handling.
 * The icon is sized to match the font size and marked as decorative with aria-hidden attribute.
 *
 * @param icon The Vaadin icon to display at the start of the layout
 * @param text The text content to display next to the icon
 * @return A configured HorizontalLayout with the icon and text arranged horizontally with minimal spacing
 */
private fun metaLine(icon: VaadinIcon, text: String): HorizontalLayout =
    HorizontalLayout(
        Icon(icon).apply {
            setSize("var(--lumo-font-size-s)")
            style["color"] = "var(--lumo-secondary-text-color)"
            style["flex"] = "0 0 auto"
            element.setAttribute("aria-hidden", "true")
        },
        Span(text).apply {
            style["color"] = "var(--lumo-secondary-text-color)"; style["font-size"] = "var(--lumo-font-size-s)"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
            // flex:1 + min-width:0 let the text shrink and ellipsize instead of overflowing under the badge.
            style["flex"] = "1"; style["min-width"] = "0"
        },
    ).apply {
        isPadding = false; isSpacing = false
        style["gap"] = "0.4rem"
        style["align-items"] = "center"
        // Fill the details column and allow the row itself to shrink so its text span can truncate.
        width = "100%"; style["min-width"] = "0"; style["overflow"] = "hidden"
    }

/**
 * The product link rendered as a small external-link icon. Opens in a new tab; its click is stopped
 * from bubbling so tapping it doesn't also trigger the card's open-detail handler.
 */
private fun beanCardLinkIcon(href: String): Anchor =
    Anchor(href, "").apply {
        setTarget("_blank")
        element.setAttribute("rel", "noopener noreferrer")
        element.setAttribute("aria-label", "Open product link")
        element.setAttribute("title", "Open product link")
        style["display"] = "inline-flex"; style["align-items"] = "center"; style["flex-shrink"] = "0"
        add(Icon(VaadinIcon.INFO_CIRCLE).apply {
            setSize("0.95rem"); style["color"] = "var(--lumo-primary-text-color)"
        })
        element.addEventListener("click") { }.stopPropagation()
    }
