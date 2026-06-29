package eu.zeletrik.beanbook.security

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.web.webauthn.api.AuthenticatorTransport
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import java.time.Instant

/**
 * Exercises the JDBC-backed WebAuthn repositories against the real SQLite schema (V3 migration). This
 * is the part most likely to break: column names, blob/boolean/timestamp affinities, and the
 * transports round-trip all have to line up with what Spring Security's repositories emit. Timestamps
 * are millisecond-precise on purpose — the xerial driver stores epoch millis, so sub-milli would drift.
 */
@SpringBootTest(
    properties = [
        "beanbook.security.enabled=true",
        "beanbook.security.username=tester",
        "beanbook.security.password=s3cret-never-used",
        // In-memory SQLite so the test is isolated and never touches the on-disk dev database, matching
        // BeanPurchaseRepositoryTest. Liquibase builds the schema (incl. the V3 WebAuthn tables) here.
        "spring.datasource.url=jdbc:sqlite::memory:?journal_mode=WAL",
    ],
)
class WebAuthnRepositoryTest(
    @Autowired private val userEntities: PublicKeyCredentialUserEntityRepository,
    @Autowired private val credentials: UserCredentialRepository,
) {

    @Test
    fun `a passkey user entity round-trips through SQLite`() {
        val id = Bytes.random()
        userEntities.save(
            ImmutablePublicKeyCredentialUserEntity.builder()
                .id(id).name("entity-tester").displayName("Entity Tester").build(),
        )

        val byId = userEntities.findById(id) ?: error("saved user entity must be findable by id")
        assertEquals("entity-tester", byId.name)
        assertEquals("Entity Tester", byId.displayName)

        val byName = userEntities.findByUsername("entity-tester")
            ?: error("saved user entity must be findable by username")
        assertEquals(id, byName.id)
    }

    @Test
    fun `a credential record round-trips through SQLite with all fields intact`() {
        val userId = Bytes.random()
        userEntities.save(
            ImmutablePublicKeyCredentialUserEntity.builder()
                .id(userId).name("cred-owner").displayName("Cred Owner").build(),
        )

        val credentialId = Bytes.random()
        val created = Instant.ofEpochMilli(1_700_000_000_000)
        val lastUsed = Instant.ofEpochMilli(1_700_000_500_000)
        val transports = setOf(AuthenticatorTransport.INTERNAL, AuthenticatorTransport.HYBRID)
        credentials.save(
            ImmutableCredentialRecord.builder()
                .credentialType(PublicKeyCredentialType.PUBLIC_KEY)
                .credentialId(credentialId)
                .userEntityUserId(userId)
                .publicKey(ImmutablePublicKeyCose(byteArrayOf(1, 2, 3, 4, 5)))
                .signatureCount(42L)
                .uvInitialized(true)
                .backupEligible(true)
                .backupState(false)
                .transports(transports)
                .created(created)
                .lastUsed(lastUsed)
                .label("My Laptop")
                .build(),
        )

        val found = credentials.findByCredentialId(credentialId)
            ?: error("saved credential must be findable by id")
        assertEquals(credentialId, found.credentialId)
        assertEquals(userId, found.userEntityUserId)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), found.publicKey.bytes)
        assertEquals(42L, found.signatureCount)
        assertTrue(found.isUvInitialized)
        assertTrue(found.isBackupEligible)
        assertEquals("My Laptop", found.label)
        assertEquals(created, found.created)
        assertEquals(lastUsed, found.lastUsed)
        assertEquals(transports, found.transports)

        val byUser = credentials.findByUserId(userId)
        assertEquals(1, byUser.size, "the owner must have exactly the one credential we saved")
        assertEquals(credentialId, byUser[0].credentialId)
    }
}
