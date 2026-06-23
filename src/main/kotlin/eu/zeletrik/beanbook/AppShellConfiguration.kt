package eu.zeletrik.beanbook

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Inline
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.theme.lumo.Lumo

@StyleSheet(Lumo.STYLESHEET)
class AppShellConfiguration : AppShellConfigurator {
    override fun configurePage(settings: AppShellSettings) {
        settings.addMetaTag("color-scheme", "light dark")
        settings.addMetaTag("viewport", "width=device-width, initial-scale=1.0, viewport-fit=cover")
        settings.addLink("manifest", "/manifest.webmanifest")
        settings.addInlineWithContents(
            "if ('serviceWorker' in navigator) { navigator.serviceWorker.register('/sw.js'); }",
            Inline.Wrapping.JAVASCRIPT
        )
        settings.addInlineWithContents(THEME_CSS, Inline.Wrapping.STYLESHEET)
    }

    companion object {
        // Light mode: Roaster Amber primary (#C27A2E).
        // Dark mode: Espresso base (#1A0F0A) with warm cream text (#F5E6C8).
        // CSS custom properties inherit into Vaadin shadow DOM, so :root overrides
        // propagate into all Lumo components.
        private val THEME_CSS = """
            /* Light mode — Almond Hearth */
            :root {
              --lumo-primary-color: #EED3BA;
              --lumo-primary-color-50pct: rgba(194, 122, 46, 0.5);
              --lumo-primary-color-10pct: rgba(194, 122, 46, 0.1);
              --lumo-primary-contrast-color: #2C1810;
              --lumo-primary-text-color: #EED3BA;
            }

            /* Dark mode — Mahogany Dusk */
            html[theme~="dark"] body,
            html[theme~="dark"] {
              background-color: #241410;
            }
            [theme~="dark"] {
              --lumo-base-color: #48261D;
              --lumo-contrast: rgba(255, 250, 245, 1);
              --lumo-contrast-90pct: rgba(255, 250, 245, 0.9);
              --lumo-contrast-70pct: rgba(255, 250, 245, 0.7);
              --lumo-contrast-50pct: rgba(255, 250, 245, 0.5);
              --lumo-contrast-30pct: rgba(255, 250, 245, 0.3);
              --lumo-contrast-20pct: rgba(255, 250, 245, 0.2);
              --lumo-contrast-10pct: rgba(255, 250, 245, 0.1);
              --lumo-contrast-5pct:  rgba(255, 250, 245, 0.05);
              --lumo-body-text-color: rgba(255, 250, 245, 0.87);
              --lumo-secondary-text-color: rgba(255, 250, 245, 0.60);
              --lumo-tertiary-text-color: rgba(255, 250, 245, 0.40);
              --lumo-disabled-text-color: rgba(255, 250, 245, 0.25);
            }
        """.trimIndent()
    }
}
