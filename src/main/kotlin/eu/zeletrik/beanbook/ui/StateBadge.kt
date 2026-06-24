package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import eu.zeletrik.beanbook.beans.BagState

/**
 * The Sealed / Open / Finished pill, shared by the list card and the detail view (the only places
 * that rendered it). [small] selects the compact variant used on list cards.
 *
 * Each state carries a distinct icon as well as a colour, so the three states stay distinguishable
 * without relying on colour alone (WCAG: don't convey meaning by colour only). The `${color}22`
 * suffix is an 8-digit-hex alpha (~13%) tint of the foreground colour.
 */
internal fun bagStateBadge(state: BagState, small: Boolean = false): Span {
    val (label, color, icon) = when (state) {
        BagState.SEALED   -> Triple("Sealed", "var(--lumo-contrast-60pct)", VaadinIcon.LOCK)
        BagState.OPEN     -> Triple("Open", "var(--lumo-success-color)", VaadinIcon.UNLOCK)
        BagState.FINISHED -> Triple("Finished", "var(--lumo-primary-color)", VaadinIcon.CHECK)
    }
    val iconComponent = Icon(icon).apply {
        style["width"] = "0.85em"; style["height"] = "0.85em"; style["color"] = color
        element.setAttribute("aria-hidden", "true")
    }
    return Span(iconComponent, Span(label)).apply {
        style["display"] = "inline-flex"
        style["align-items"] = "center"
        style["gap"] = "0.25rem"
        style["background"] = "${color}22"
        style["color"] = color
        style["border-radius"] = "var(--lumo-border-radius-m)"
        style["font-weight"] = "600"
        style["padding"] = if (small) "0.1rem 0.5rem" else "0.1rem 0.6rem"
        style["font-size"] = if (small) "var(--lumo-font-size-xs)" else "var(--lumo-font-size-s)"
    }
}
