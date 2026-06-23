package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.Notification.Position
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

/**
 * Shows Lumo-themed [Notification] toasts (success, success-with-undo, error) anchored to the
 * bottom centre of the screen.
 */
object NotificationHelper {

    private const val SUCCESS_DURATION_MS = 3000

    /** Undo toasts linger longer than a plain success so the user has time to react. */
    private const val UNDO_DURATION_MS = 6000

    /**
     * Test seam: tests install a recorder to capture (text, isError) without searching the Karibu
     * overlay. Null in production, so nothing is retained (the old always-on list grew unbounded).
     */
    internal var recorder: ((text: String, isError: Boolean) -> Unit)? = null

    fun success(text: String) {
        recorder?.invoke(text, false)
        Notification.show(text, SUCCESS_DURATION_MS, Position.BOTTOM_CENTER).apply {
            addThemeVariants(NotificationVariant.LUMO_SUCCESS)
        }
    }

    /**
     * Success toast with an inline "Undo" action. Recorded as a non-error toast so existing
     * notification assertions (which only inspect text + isError) keep matching.
     */
    fun successWithUndo(text: String, onUndo: () -> Unit) {
        recorder?.invoke(text, false)
        val notification = Notification().apply {
            position = Position.BOTTOM_CENTER
            duration = UNDO_DURATION_MS
            addThemeVariants(NotificationVariant.LUMO_SUCCESS)
        }
        val undoButton = Button("Undo") {
            onUndo()
            notification.close()
        }.apply {
            addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE)
            element.setAttribute("aria-label", "Undo")
        }
        notification.add(HorizontalLayout(
            com.vaadin.flow.component.html.Span(text),
            undoButton,
        ).apply {
            style["align-items"] = "center"
            style["gap"] = "0.5rem"
        })
        notification.open()
    }

    fun error(text: String) {
        recorder?.invoke(text, true)
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
