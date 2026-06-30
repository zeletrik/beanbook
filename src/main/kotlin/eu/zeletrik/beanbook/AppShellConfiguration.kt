package eu.zeletrik.beanbook

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Inline
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.theme.lumo.Lumo

/**
 * Configures the application's HTML shell: PWA meta tags, the web manifest link, service-worker
 * registration, and the inlined Lumo theme overrides.
 */
@StyleSheet(Lumo.STYLESHEET)
class AppShellConfiguration : AppShellConfigurator {
    override fun configurePage(settings: AppShellSettings) {
        settings.addMetaTag("color-scheme", "light dark")
        settings.addMetaTag("viewport", "width=device-width, initial-scale=1.0, viewport-fit=cover")
        // Safari reads this for the toolbar tint; iOS standalone falls back to the manifest theme_color.
        settings.addMetaTag("theme-color", "#120a07")
        settings.addLink("manifest", "/manifest.webmanifest")
        settings.addLink("/icons/favicon.svg", mapOf("rel" to "icon", "type" to "image/svg+xml"))
        // iOS ignores the manifest icons and can't use SVG for the home-screen icon — it needs a raster
        // apple-touch-icon (otherwise "Add to Home Screen" uses a screenshot of the page).
        settings.addLink("apple-touch-icon", "/icons/apple-touch-icon.png")
        // iOS standalone PWA: full-screen launch, home-screen title, and a dark status bar for the espresso theme.
        settings.addMetaTag("mobile-web-app-capable", "yes")
        settings.addMetaTag("apple-mobile-web-app-capable", "yes")
        settings.addMetaTag("apple-mobile-web-app-title", "Bean Book")
        settings.addMetaTag("apple-mobile-web-app-status-bar-style", "black")
        settings.addInlineWithContents(SERVICE_WORKER_REGISTRATION, Inline.Wrapping.JAVASCRIPT)
        settings.addInlineWithContents(THEME_CSS, Inline.Wrapping.STYLESHEET)
    }

    companion object {
        /**
         * Registers the service worker. A new worker calls skipWaiting()+clients.claim(), which
         * swaps the controller of the live page; reload only when the page was already controlled
         * (an update) so the first visit isn't reloaded under the user.
         */
        private val SERVICE_WORKER_REGISTRATION = """
            if ('serviceWorker' in navigator) {
              const hadController = !!navigator.serviceWorker.controller;
              navigator.serviceWorker.addEventListener('controllerchange', () => {
                if (hadController) window.location.reload();
              });
              navigator.serviceWorker.register('/sw.js');
            }
        """.trimIndent()

        /**
         * Inlined Lumo theme overrides — the "Neural Roast" palette drawn from the app icon
         * (warm coffee roast + cool AI/tech on an espresso base).
         *
         * CSS custom properties inherit into Vaadin shadow DOM, so :root overrides
         * propagate into all Lumo components.
         */
        private val THEME_CSS = """
            /* Tabular figures: align digit columns app-wide (prices, weights, ratings).
               font-variant-numeric inherits, so setting it on the root crosses into
               the Lumo component shadow DOM. */
            html, body {
              font-variant-numeric: tabular-nums;
            }

            /* AI-suggested fields (auto-fill from photo): a cool cyan/blue left accent ties the
               auto-fill cue to the app's "AI" identity (the icon's tech half + spark). Host-level
               box-shadow needs no shadow-DOM piercing. Removed as soon as the user edits. */
            .ai-suggested {
              border-radius: 2px;
              box-shadow: -3px 0 0 0 rgba(31, 117, 254, 0.7);
            }
            
            html[theme~="light"] body,
            html[theme~="light"] {
              background-color: #FAF5EF;
            }
            [theme~="light"] {
                --lumo-primary-color: #E76F51;
            }
            
            html[theme~="dark"] body,
            html[theme~="dark"] {
              background-color: #010208;
            }
            [theme~="dark"] {
              --lumo-base-color: #030612;
              --lumo-primary-color: #F07A55;
              --lumo-contrast-90pct: rgba(255, 253, 240, 0.9);
              --lumo-contrast-70pct: rgba(255, 253, 240, 0.7);
              --lumo-contrast-50pct: rgba(255, 253, 240, 0.5);
              --lumo-contrast-30pct: rgba(255, 253, 240, 0.3);
              --lumo-contrast-20pct: rgba(255, 253, 240, 0.2);
              --lumo-contrast-10pct: rgba(255, 253, 240, 0.1);
              --lumo-contrast-5pct:  rgba(255, 253, 240, 0.05);
              --lumo-body-text-color: rgba(255, 253, 240, 0.94);
              --lumo-secondary-text-color: rgba(255, 253, 240, 0.72);
              --lumo-tertiary-text-color: rgba(255, 253, 240, 0.48);
              --lumo-disabled-text-color: rgba(255, 253, 240, 0.30);
            }
        """.trimIndent()
    }
}
