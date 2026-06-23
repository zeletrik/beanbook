package eu.zeletrik.beanbook.wishlist

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WishlistService(private val jdbcTemplate: JdbcTemplate) {

    fun findAll(): List<WishlistItem> = jdbcTemplate.query(
        "SELECT id, name, roaster, origin, notes FROM wishlist_items ORDER BY rowid"
    ) { rs, _ ->
        WishlistItem(
            id = UUID.fromString(rs.getString("id")),
            name = rs.getString("name"),
            roaster = rs.getString("roaster"),
            origin = rs.getString("origin"),
            notes = rs.getString("notes"),
        )
    }

    fun upsert(item: WishlistItem) {
        jdbcTemplate.update(
            "INSERT OR REPLACE INTO wishlist_items (id, name, roaster, origin, notes) VALUES (?, ?, ?, ?, ?)",
            item.id.toString(),
            item.name,
            item.roaster,
            item.origin,
            item.notes,
        )
    }

    fun deleteById(id: UUID) {
        jdbcTemplate.update("DELETE FROM wishlist_items WHERE id = ?", id.toString())
    }
}
