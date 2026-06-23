package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.Notification.Position
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

object NotificationHelper {

    // Test seam: captures (text, isError) pairs so tests can assert without Karibu overlay search.
    internal val _shown = mutableListOf<Pair<String, Boolean>>()

    fun success(text: String) {
        _shown.add(text to false)
        Notification.show(text, 3000, Position.BOTTOM_CENTER).apply {
            addThemeVariants(NotificationVariant.LUMO_SUCCESS)
        }
    }

    fun error(text: String) {
        _shown.add(text to true)
        val notification = Notification().apply {
            position = Position.BOTTOM_CENTER
            duration = 0
            addThemeVariants(NotificationVariant.LUMO_ERROR)
        }
        val closeButton = Button("×") { notification.close() }.apply {
            addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE)
            element.setAttribute("aria-label", "Close")
        }
        notification.add(HorizontalLayout(
            com.vaadin.flow.component.html.Span(text),
            closeButton,
        ).apply {
            style["align-items"] = "center"
            style["gap"] = "0.5rem"
        })
        notification.open()
    }
}
