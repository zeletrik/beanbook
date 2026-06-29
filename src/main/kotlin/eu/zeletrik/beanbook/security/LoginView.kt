package eu.zeletrik.beanbook.security

import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinServletRequest
import com.vaadin.flow.server.auth.AnonymousAllowed
import org.springframework.security.web.csrf.CsrfToken

/**
 * Login page shown by Spring Security when authentication is enabled. Offers two ways in:
 *
 * - The [LoginForm] posts username/password to the `login` URL that [SecurityConfig] wires up via
 *   Vaadin's security configurer.
 * - The passkey button drives Spring Security's WebAuthn ceremony. Rather than hand-write the
 *   base64url + `navigator.credentials` dance, we load Spring's own `login/webauthn.js` and call its
 *   `setupLogin(headers, contextPath, button)`, which binds the click handler and redirects on success.
 *   The WebAuthn endpoints are CSRF-protected (Vaadin only exempts its own framework requests), so we
 *   read the session CSRF token server-side and hand it to the script as a request header.
 *
 * Annotated [AnonymousAllowed] so it's reachable without being signed in.
 */
@Route("login")
@PageTitle("Sign in · Bean Book")
@AnonymousAllowed
class LoginView : VerticalLayout(), BeforeEnterObserver {

    private val login = LoginForm().apply {
        action = "login"
    }

    private val passkeyButton = Button("Sign in with a passkey", VaadinIcon.KEY.create()).apply {
        setId(PASSKEY_BUTTON_ID)
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        setWidthFull()
    }

    init {
        setSizeFull()
        alignItems = FlexComponent.Alignment.CENTER
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER

        val card = VerticalLayout(login, orDivider(), passkeyButton).apply {
            isPadding = false
            isSpacing = true
            width = "auto"
            alignItems = FlexComponent.Alignment.STRETCH
        }
        add(card)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (event.location.queryParameters.parameters.containsKey("error")) {
            login.isError = true
        }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        applySystemTheme(attachEvent)
        wirePasskeyLogin()
    }

    /**
     * Match the OS light/dark preference, like the main app does. The login page is its own route and
     * never runs MainView's theme setup, so without this it would always render in the light theme.
     * Mirrors MainView.applySystemTheme (the two live in different modules and can't share a helper).
     */
    private fun applySystemTheme(attachEvent: AttachEvent) {
        attachEvent.ui.page.executeJs(
            """
            document.documentElement.style.height = '100dvh';
            document.body.style.height = '100dvh';
            const apply = (dark) => { document.documentElement.setAttribute('theme', dark ? 'dark' : ''); };
            const mq = window.matchMedia('(prefers-color-scheme: dark)');
            apply(mq.matches);
            if (!window.__beanbookThemeBound) {
                window.__beanbookThemeBound = true;
                mq.addEventListener('change', (e) => apply(e.matches));
            }
            """.trimIndent(),
        )
    }

    /**
     * Loads Spring's WebAuthn script and binds it to the passkey button once loaded. The button is
     * hidden when the browser has no WebAuthn support so it never dead-ends. Passing the CSRF token as
     * a header lets the script's POSTs to `/webauthn/authenticate/options` and `/login/webauthn` pass
     * Spring's CSRF check.
     */
    private fun wirePasskeyLogin() {
        val request = (VaadinServletRequest.getCurrent())?.httpServletRequest
        val contextPath = request?.contextPath ?: ""
        val csrf = request?.getAttribute(CsrfToken::class.java.name) as? CsrfToken
        val scriptUrl = "$contextPath/login/webauthn.js"

        passkeyButton.element.executeJs(
            """
            const button = this;
            if (!window.PublicKeyCredential) { button.style.display = 'none'; return; }
            const headers = {};
            if ($1 && $2) { headers[$1] = $2; }
            const script = document.createElement('script');
            script.src = $0;
            script.onload = () => window.setupLogin(headers, $3, button);
            script.onerror = () => { button.disabled = true; console.error('Failed to load passkey support'); };
            document.head.appendChild(script);
            """.trimIndent(),
            scriptUrl,
            csrf?.headerName,
            csrf?.token,
            contextPath,
        )
    }

    /** An "or" separator between the password form and the passkey button. */
    private fun orDivider(): Span = Span("or").apply {
        style["align-items"] = "center"
        style["text-align"] = "center"
        style["color"] = "var(--lumo-secondary-text-color)"
        style["font-size"] = "var(--lumo-font-size-s)"
        style["gap"] = "var(--lumo-space-s)"
        style["margin"] = "var(--lumo-space-s) 0"
    }

    companion object {
        const val PASSKEY_BUTTON_ID = "passkey-signin"
    }
}
