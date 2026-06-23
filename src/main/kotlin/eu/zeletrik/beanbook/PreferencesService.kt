package eu.zeletrik.beanbook

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class PreferencesService(private val jdbcTemplate: JdbcTemplate) {

    fun getCurrency(): String = jdbcTemplate.query(
        "SELECT value FROM user_preferences WHERE key = 'currency'"
    ) { rs, _ -> rs.getString("value") }.firstOrNull() ?: "€"

    fun setCurrency(symbol: String) {
        jdbcTemplate.update(
            "INSERT OR REPLACE INTO user_preferences (key, value) VALUES ('currency', ?)",
            symbol,
        )
    }
}
