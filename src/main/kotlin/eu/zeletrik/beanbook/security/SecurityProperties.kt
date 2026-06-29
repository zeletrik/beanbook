package eu.zeletrik.beanbook.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for the optional, off-by-default authentication, bound from `beanbook.security.*`.
 *
 * When [enabled] is `false` (the default) the app stays open — appropriate for a loopback-only
 * deployment. Enabling it requires a single [username] / [password], supplied via the environment
 * (`BEANBOOK_AUTH_USERNAME` / `BEANBOOK_AUTH_PASSWORD`) and never baked into the image. Passkey
 * (WebAuthn) login is available alongside the password whenever auth is enabled; see [passkey].
 */
@ConfigurationProperties(prefix = "beanbook.security")
data class SecurityProperties(
    /** Master switch; when `false` every request is permitted and no login is required. */
    val enabled: Boolean = false,
    /** The single account's username; required when [enabled]. */
    val username: String = "",
    /** The single account's password, from the environment; required when [enabled]. */
    val password: String? = null,
    /** Relying-party settings for passkey (WebAuthn) login. */
    val passkey: Passkey = Passkey(),
)

/**
 * WebAuthn relying-party settings, bound as the nested `beanbook.security.passkey.*` block of
 * [SecurityProperties]. Passkeys are origin-bound, so these must match how the app is actually reached.
 * The defaults suit loopback use; when exposing the app set [rpId] to the host (e.g. `beans.example.com`)
 * and [allowedOrigins] to the full origin(s) (e.g. `https://beans.example.com`) via
 * `BEANBOOK_PASSKEY_RP_ID` / `BEANBOOK_PASSKEY_ALLOWED_ORIGINS`.
 *
 * Carries its own [@ConfigurationProperties][ConfigurationProperties] purely so the IDE resolves the
 * nested `beanbook.security.passkey.*` keys in `application.yml` (this project has no
 * config-metadata processor; IntelliJ keys off the annotation's prefix). Runtime binding happens via
 * [SecurityProperties.passkey]; see [SecurityConfig] for why it's also in `@EnableConfigurationProperties`.
 */
@ConfigurationProperties(prefix = "beanbook.security.passkey")
data class Passkey(
    /** The relying-party id — the registrable domain the app is served from. */
    val rpId: String = "localhost",
    /** Human-readable relying-party name shown by the authenticator during registration. */
    val rpName: String = "Bean Book",
    /** Origins the browser is allowed to present passkeys from. */
    val allowedOrigins: Set<String> = setOf("http://localhost:8001"),
)
