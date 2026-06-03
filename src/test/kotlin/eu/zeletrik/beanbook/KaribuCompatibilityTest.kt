package eu.zeletrik.beanbook

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KaribuCompatibilityTest {

    @BeforeEach
    fun setup() {
        MockVaadin.setup()
    }

    @AfterEach
    fun cleanup() {
        MockVaadin.tearDown()
    }

    @Test
    fun `karibu initialises with vaadin 25 without errors`() {
        val layout = VerticalLayout()
        layout.add(Span("smoke test"))
    }
}
