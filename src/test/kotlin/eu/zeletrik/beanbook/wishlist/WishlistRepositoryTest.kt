package eu.zeletrik.beanbook.wishlist

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/** Integration tests for [WishlistService] persistence, covering upsert ordering, deletion, and nullable field round-trips. */
@SpringBootTest(properties = ["spring.datasource.url=jdbc:sqlite::memory:?journal_mode=WAL"])
class WishlistRepositoryTest {

    @Autowired
    private lateinit var service: WishlistService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun clearTable() {
        jdbcTemplate.execute("DELETE FROM wishlist_items")
    }

    private fun item(name: String) = WishlistItem(UUID.randomUUID(), name, "Roaster", "Origin", "tasting notes")

    @Test
    fun `upsert inserts and findAll returns items in insertion order`() {
        service.upsert(item("Alpha"))
        service.upsert(item("Beta"))
        assertEquals(listOf("Alpha", "Beta"), service.findAll().map { it.name })
    }

    // The core §A3.4 fix: a proper UPDATE preserves rowid, so order survives — the old
    // INSERT OR REPLACE delete-and-reinserted, reordering the list on every edit.
    @Test
    fun `upsert with existing id updates in place without duplicating or reordering`() {
        val alpha = item("Alpha")
        service.upsert(alpha)
        service.upsert(item("Beta"))

        service.upsert(alpha.copy(name = "Alpha v2"))

        val all = service.findAll()
        assertEquals(2, all.size, "updating an existing id must not create a second row")
        assertEquals(listOf("Alpha v2", "Beta"), all.map { it.name }, "insertion order must survive the update")
    }

    @Test
    fun `deleteById removes the item`() {
        val alpha = item("Alpha")
        service.upsert(alpha)
        service.deleteById(alpha.id)
        assertTrue(service.findAll().isEmpty())
    }

    @Test
    fun `notes round-trip including null`() {
        service.upsert(item("WithNotes"))
        service.upsert(WishlistItem(UUID.randomUUID(), "NoNotes", "R", "O", null))
        val byName = service.findAll().associateBy { it.name }
        assertEquals("tasting notes", byName["WithNotes"]?.notes)
        assertNull(byName["NoNotes"]?.notes)
    }

    @Test
    fun `url round-trip including null`() {
        service.upsert(WishlistItem(UUID.randomUUID(), "WithUrl", "R", "O", null, "https://roaster.com/bean"))
        service.upsert(WishlistItem(UUID.randomUUID(), "NoUrl", "R", "O", null, null))
        val byName = service.findAll().associateBy { it.name }
        assertEquals("https://roaster.com/bean", byName["WithUrl"]?.url)
        assertNull(byName["NoUrl"]?.url)
    }
}
