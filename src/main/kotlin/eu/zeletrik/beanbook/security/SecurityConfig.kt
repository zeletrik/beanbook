package eu.zeletrik.beanbook.security

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository

/**
 * Optional authentication, off by default.
 *
 * - `beanbook.security.enabled=false` (default): every request is permitted and no login is required.
 *   The chain still goes through Vaadin's security configurer (so CSRF keeps being handled the way
 *   Vaadin handles it for the SPA — not disabled), but navigation access control is off, so the app is
 *   as open as it was before Spring Security was on the classpath.
 * - `beanbook.security.enabled=true`: Vaadin form login against a single in-memory account, with the
 *   credentials supplied via the environment. Fails fast at startup if they're missing. Passkey
 *   (WebAuthn) login is wired up alongside the password, with credentials persisted to SQLite.
 */
@Configuration
@EnableWebSecurity
// Passkey is registered alongside SecurityProperties so the IDE resolves the nested
// `beanbook.security.passkey.*` keys (no config-metadata processor in this project). It's a tiny extra
// bean; runtime binding still flows through SecurityProperties.passkey.
@EnableConfigurationProperties(SecurityProperties::class, Passkey::class)
class SecurityConfig {

    @Bean
    @ConditionalOnBooleanProperty("beanbook.security.enabled")
    fun securedFilterChain(
        http: HttpSecurity,
        properties: SecurityProperties,
        vaadinSecurityContextHolderStrategy: SecurityContextHolderStrategy,
    ): SecurityFilterChain {
        SecurityContextHolder.setContextHolderStrategy(vaadinSecurityContextHolderStrategy)
        http.authorizeHttpRequests {
            // The login ceremony runs pre-auth (anonymous): the options endpoint, the assertion POST, and
            // the ceremony script LoginView loads. The `.js` is actually served by Spring's
            // DefaultResourcesFilter before authorization runs, so permitting it here is belt-and-braces
            // — but it documents the intent and keeps it anonymous if that default matcher ever changes.
            it.requestMatchers("/webauthn/authenticate/options", "/login/webauthn", "/login/webauthn.js").permitAll()
            it.requestMatchers("/webauthn/register", "/webauthn/register/**").authenticated()
        }
        http.with(VaadinSecurityConfigurer.vaadin()) { it.loginView(LoginView::class.java) }
        http.webAuthn {
            it.rpName(properties.passkey.rpName)
            it.rpId(properties.passkey.rpId)
            it.allowedOrigins(properties.passkey.allowedOrigins)
        }
        return http.build()
    }

    /**
     * Persisted store of the WebAuthn user entities (one per account). Backed by the app's SQLite
     * datasource so registered passkeys survive restarts; the `user_entities` table ships in the V3
     * migration.
     */
    @Bean
    @ConditionalOnBooleanProperty("beanbook.security.enabled")
    fun publicKeyCredentialUserEntityRepository(jdbc: JdbcOperations): PublicKeyCredentialUserEntityRepository =
        JdbcPublicKeyCredentialUserEntityRepository(jdbc)

    /** Persisted store of registered passkey credentials (`user_credentials` table, V3 migration). */
    @Bean
    @ConditionalOnBooleanProperty("beanbook.security.enabled")
    fun userCredentialRepository(jdbc: JdbcOperations): UserCredentialRepository =
        JdbcUserCredentialRepository(jdbc)

    @Bean
    @ConditionalOnBooleanProperty("beanbook.security.enabled")
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    @ConditionalOnBooleanProperty("beanbook.security.enabled")
    fun userDetailsService(properties: SecurityProperties, encoder: PasswordEncoder): UserDetailsService {
        val password = properties.password?.takeIf { it.isNotBlank() }
        require(properties.username.isNotBlank() && password != null) {
            "beanbook.security.enabled=true but no credentials (set BEANBOOK_AUTH_USERNAME / BEANBOOK_AUTH_PASSWORD)"
        }
        val user = User.withUsername(properties.username)
            .password(encoder.encode(password))
            .roles("USER")
            .build()
        return InMemoryUserDetailsManager(user)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "beanbook.security",
        name = ["enabled"],
        havingValue = "false",
        matchIfMissing = true
    )
    fun openFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.with(VaadinSecurityConfigurer.vaadin()) {
            it.enableNavigationAccessControl(false)
            it.anyRequest { request -> request.permitAll() }
        }
        return http.build()
    }
}
