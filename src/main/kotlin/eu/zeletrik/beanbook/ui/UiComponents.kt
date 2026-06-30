package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout

/**
 * Shared visual building blocks for the app's "card" language — a tinted icon circle and a rounded
 * section card. Used by the Settings page and the purchase form so both read as the same product.
 * ([AnalyticsPanel.section] keeps its own variant: it adds a min-height floor + vertical centering
 * that the plain card doesn't need.)
 */

/** A small circular badge holding [icon], tinted with a translucent wash of [color]. */
internal fun iconCircle(
    icon: VaadinIcon,
    color: String,
    iconSize: String = "1.4rem",
    circleSize: String = "2.2rem",
): Div =
    Div(Icon(icon).apply {
        style["color"] = color; style["width"] = iconSize; style["height"] = iconSize
    }).apply {
        style["width"] = circleSize; style["height"] = circleSize; style["border-radius"] = "50%"
        style["background"] = "${color}1a" // ~10% alpha over the accent
        style["display"] = "flex"; style["align-items"] = "center"; style["justify-content"] = "center"
        style["flex-shrink"] = "0"
    }

/**
 * A rounded surface card with an [icon]-circle + [title] header (and optional muted [description]),
 * followed by [content]. The body stretches its children to full width, so a [com.vaadin.flow.component.formlayout.FormLayout]
 * or full-width control fills the card.
 */
internal fun sectionCard(
    icon: VaadinIcon,
    iconColor: String,
    title: String,
    description: String? = null,
    vararg content: Component,
): Div {
    val heading = VerticalLayout(
        H3(title).apply { style["margin"] = "0"; style["font-size"] = "var(--lumo-font-size-m)" },
    ).apply {
        isPadding = false; isSpacing = false; style["gap"] = "0.1rem"; style["min-width"] = "0"
        description?.let {
            add(Span(it).apply {
                style["color"] = "var(--lumo-secondary-text-color)"; style["font-size"] = "var(--lumo-font-size-s)"
            })
        }
    }
    val header = HorizontalLayout(iconCircle(icon, iconColor), heading).apply {
        isPadding = false; isSpacing = true; width = "100%"; style["align-items"] = "center"
    }
    val body = VerticalLayout(*content).apply {
        isPadding = false; isSpacing = false; style["gap"] = "0.85rem"
        width = "100%"; style["align-items"] = "stretch"
    }
    return Div(header, body).apply {
        style["display"] = "flex"; style["flex-direction"] = "column"; style["gap"] = "0.85rem"
        style["width"] = "100%"; style["box-sizing"] = "border-box"
        style["padding"] = "1rem"
        style["border-radius"] = "var(--lumo-border-radius-l)"
        style["background"] = "var(--lumo-base-color)"
        style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
    }
}
