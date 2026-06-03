package eu.zeletrik.beanbook

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.theme.lumo.Lumo

@StyleSheet(Lumo.STYLESHEET)
class AppShellConfiguration : AppShellConfigurator {
    override fun configurePage(settings: AppShellSettings) {
        settings.addMetaTag("color-scheme", "light dark")
        settings.addMetaTag("viewport", "width=device-width, initial-scale=1.0, viewport-fit=cover")
        settings.addLink("manifest", "/manifest.webmanifest")
    }
}
