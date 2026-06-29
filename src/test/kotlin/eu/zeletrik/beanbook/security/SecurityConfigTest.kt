package eu.zeletrik.beanbook.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * With authentication enabled (credentials supplied via properties, as they'd be via the environment),
 * the in-memory account and the password encoder must wire into the context, and the encoded password
 * must verify. Guards against regressions in [SecurityConfig] that the default flag-off tests can't catch.
 */
@SpringBootTest(
    properties = [
        "beanbook.security.enabled=true",
        "beanbook.security.username=tester",
        "beanbook.security.password=s3cret-never-used",
    ],
)
class SecurityEnabledContextTest(
    @Autowired private val userDetailsService: UserDetailsService?,
    @Autowired private val passwordEncoder: PasswordEncoder?,
) {

    @Test
    fun `the security beans are created and the configured account verifies when auth is enabled`() {
        assertNotNull(userDetailsService, "UserDetailsService must exist when beanbook.security.enabled=true")
        assertNotNull(passwordEncoder, "PasswordEncoder must exist when beanbook.security.enabled=true")

        val user = userDetailsService!!.loadUserByUsername("tester")
        assertEquals("tester", user.username)
        assertTrue(user.authorities.any { it.authority == "ROLE_USER" }, "configured account must have ROLE_USER")
        assertTrue(
            passwordEncoder!!.matches("s3cret-never-used", user.password),
            "the stored password must be the BCrypt encoding of the configured one",
        )
    }
}

/**
 * With auth disabled, our authentication must not be wired up: the [PasswordEncoder] we create only
 * when `beanbook.security.enabled=true` must be absent, so the open filter chain is in effect and the
 * app behaves as it did before Spring Security was on the classpath. The flag is pinned explicitly so
 * the test is hermetic regardless of the deployment's configured default.
 */
@SpringBootTest(properties = ["beanbook.security.enabled=false"])
class SecurityDisabledContextTest(
    @Autowired private val passwordEncoder: PasswordEncoder?,
) {

    @Test
    fun `our security beans are absent when auth is disabled`() {
        assertNull(passwordEncoder, "PasswordEncoder must not exist when security is off")
    }
}
