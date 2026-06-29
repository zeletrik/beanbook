package eu.zeletrik.beanbook.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.CookieManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Guards the authorization wiring for the WebAuthn endpoints over a real Tomcat. Vaadin's security
 * configurer only whitelists its own framework/static requests and known routes, so Spring's webauthn
 * endpoints have to be authorized explicitly — without that an authenticated user gets a 403 with an
 * empty body (the symptom that looked like "the register link downloads an empty file").
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = [
        "beanbook.security.enabled=true",
        "beanbook.security.username=tester",
        "beanbook.security.password=s3cret",
        "spring.datasource.url=jdbc:sqlite::memory:?journal_mode=WAL",
    ],
)
class WebAuthnEndpointSecurityTest {

    @LocalServerPort
    private var port: Int = 0

    private val base get() = "http://localhost:$port"

    private fun newClient() = HttpClient.newBuilder()
        .cookieHandler(CookieManager())
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private fun HttpClient.get(path: String): HttpResponse<String> =
        send(HttpRequest.newBuilder(URI("$base$path")).GET().build(), HttpResponse.BodyHandlers.ofString())

    private fun HttpClient.formLogin(user: String, password: String): HttpResponse<String> =
        send(
            HttpRequest.newBuilder(URI("$base/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=$user&password=$password"))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    @Test
    fun `the passkey ceremony script is served`() {
        val resp = newClient().get("/login/webauthn.js")
        assertEquals(200, resp.statusCode(), "Spring's WebAuthn ceremony script must be served")
        assertTrue(resp.body().contains("setupLogin"), "the script must expose setupLogin")
    }

    @Test
    fun `an authenticated user can open the passkey registration page`() {
        val client = newClient()
        client.get("/login") // establish a session
        client.formLogin("tester", "s3cret")

        val resp = client.get("/webauthn/register")
        assertEquals(200, resp.statusCode(), "registration page must be reachable once signed in (was 403)")
        assertTrue(
            resp.body().contains("WebAuthn Registration"),
            "an authenticated user must get the real registration page, not a 403 or empty body",
        )
    }

    @Test
    fun `the registration page is gated behind authentication`() {
        // Anonymous access must not yield the registration page; Vaadin serves the login shell instead.
        val resp = newClient().get("/webauthn/register")
        assertFalse(
            resp.body().contains("WebAuthn Registration"),
            "the registration page must not be reachable without signing in",
        )
    }

    @Test
    fun `a signed-in user can fetch passkey registration options`() {
        // This is the step that drives "Register" on the page. It failed with HTTP 400 (empty body)
        // when the WebAuthn filters used a different SecurityContextHolderStrategy than the chain and so
        // saw an anonymous context. Posting with the page's CSRF token must now yield the creation options.
        val client = newClient()
        client.get("/login")
        client.formLogin("tester", "s3cret")

        val page = client.get("/webauthn/register").body()
        val csrf = Regex("""setupRegistration\(\s*\{\s*"([^"]+)"\s*:\s*"([^"]+)"\s*}""").find(page)
            ?: error("could not parse the CSRF header from the registration page")

        val resp = client.send(
            HttpRequest.newBuilder(URI("$base/webauthn/register/options"))
                .header(csrf.groupValues[1], csrf.groupValues[2])
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        assertEquals(200, resp.statusCode(), "register/options must return options for a signed-in user (was 400)")
        assertTrue(
            resp.body().contains("\"challenge\""),
            "the response must be PublicKeyCredentialCreationOptions JSON with a challenge",
        )
    }
}
