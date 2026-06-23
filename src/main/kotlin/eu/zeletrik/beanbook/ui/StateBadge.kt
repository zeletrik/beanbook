package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.html.Span
import eu.zeletrik.beanbook.beans.BagState

/**
 * The Sealed / Open / Finished pill, shared by the list card and the detail view (the only places
 * that rendered it). [small] selects the compact variant used on list cards. The `${color}22`
 * suffix is an 8-digit-hex alpha (~13%) tint of the foreground colour.
 */
internal fun bagStateBadge(state: BagState, small: Boolean = false): Span {
    val (label, color) = when (state) {
        BagState.SEALED   -> "Sealed"   to "var(--lumo-contrast-60pct)"
        BagState.OPEN     -> "Open"     to "var(--lumo-success-color)"
        BagState.FINISHED -> "Finished" to "var(--lumo-primary-color)"
    }
    return Span(label).apply {
        style["background"] = "${color}22"
        style["color"] = color
        style["border-radius"] = "var(--lumo-border-radius-m)"
        style["font-weight"] = "600"
        style["padding"] = if (small) "0.1rem 0.5rem" else "0.1rem 0.6rem"
        style["font-size"] = if (small) "var(--lumo-font-size-xs)" else "var(--lumo-font-size-s)"
    }
}
