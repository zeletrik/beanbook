package eu.zeletrik.beanbook.preferences

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Reads and writes user preferences (such as the display currency) stored in the `user_preferences` table. */
@Service
class PreferencesService(private val jdbcTemplate: JdbcTemplate) {

    /** Returns the configured currency symbol, falling back to the euro sign when no preference has been stored yet. */
    @Transactional(readOnly = true)
    fun getCurrency(): String = jdbcTemplate.query(
        "SELECT value FROM user_preferences WHERE key = 'currency'"
    ) { rs, _ -> rs.getString("value") }.firstOrNull() ?: "€"

    @Transactional
    fun setCurrency(symbol: String) {
        jdbcTemplate.update(
            "INSERT OR REPLACE INTO user_preferences (key, value) VALUES ('currency', ?)",
            symbol,
        )
    }
}
