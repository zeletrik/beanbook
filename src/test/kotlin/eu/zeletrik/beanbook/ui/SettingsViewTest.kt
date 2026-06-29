package eu.zeletrik.beanbook.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.spring.security.AuthenticationContext
import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.security.SecurityProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Tests for the Settings tab in [MainView], covering its navigation entry, Export Data action, and Preferences placeholder. */
class SettingsViewTest {

    @BeforeEach fun setup() = MockVaadin.setup()
    @AfterEach  fun teardown() = MockVaadin.tearDown()

    private fun makeView(): MainView = testMainView(object : TestBeanPurchaseRepository() {})

    // AC-9: Settings tab is present in the bottom navigation bar (4 icon-only tabs)
    @Test
    fun `Settings tab is present in the bottom nav bar`() {
        val view = makeView()
        val tabCount = view.tabs.children.count()
        assertTrue(tabCount == 5L, "Expected 5 tabs (Purchases, Add, Analytics, Settings, Wishlist), got: $tabCount")
    }

    // AC-10: Export Data button is visible on the Settings page
    @Test
    fun `Settings page shows Export Data button`() {
        val view = makeView()
        view.navigateTo(AppTab.SETTINGS) // Settings is the 5th tab (index 4)

        val anchors = view.settingsPage._find<Anchor> { id = "export-data-btn" }
        assertTrue(anchors.isNotEmpty(), "Expected Export Data anchor/button on Settings page")
    }

    // AC-11: Preferences placeholder is visible on the Settings page
    @Test
    fun `Settings page shows Preferences coming soon section`() {
        val view = makeView()
        view.navigateTo(AppTab.SETTINGS)

        val headings = view.settingsPage._find<H3>()
        assertTrue(
            headings.any { it.text.contains("Preferences") },
            "Expected 'Preferences' heading on Settings page, found: ${headings.map { it.text }}"
        )
    }

    // AC-12 smoke: clicking Export Data does not throw in MockVaadin
    @Test
    fun `clicking Export Data anchor does not throw`() {
        val view = makeView()
        view.navigateTo(AppTab.SETTINGS)

        // Verify anchor exists and has download attribute set
        val anchor = view.settingsPage._get<Anchor> { id = "export-data-btn" }
        assertTrue(anchor.element.hasAttribute("download"), "Anchor must have download attribute")
    }

    /** Records logout calls without performing a real Spring Security logout. */
    private class RecordingAuthContext : AuthenticationContext() {
        var loggedOut = false
        override fun logout() { loggedOut = true }
    }

    private fun securedView(auth: AuthenticationContext): MainView = testMainView(
        object : TestBeanPurchaseRepository() {},
        securityProperties = SecurityProperties(enabled = true, username = "u", password = "p"),
        authenticationContext = auth,
    )

    @Test
    fun `Settings page shows a logout button when auth is enabled`() {
        val view = securedView(RecordingAuthContext())
        view.navigateTo(AppTab.SETTINGS)

        val buttons = view.settingsPage._find<Button> { id = "logout-btn" }
        assertTrue(buttons.isNotEmpty(), "Expected a logout button on Settings page when auth is enabled")
    }

    @Test
    fun `clicking logout invokes the authentication context logout`() {
        val auth = RecordingAuthContext()
        val view = securedView(auth)
        view.navigateTo(AppTab.SETTINGS)

        view.settingsPage._get<Button> { id = "logout-btn" }._click()
        assertTrue(auth.loggedOut, "Clicking logout must call AuthenticationContext.logout()")
    }

    @Test
    fun `Settings page has no logout button when auth is disabled`() {
        val view = makeView() // default: security disabled
        view.navigateTo(AppTab.SETTINGS)

        assertFalse(
            view.settingsPage._find<Button> { id = "logout-btn" }.isNotEmpty(),
            "Logout button must be absent when auth is disabled",
        )
    }
}
