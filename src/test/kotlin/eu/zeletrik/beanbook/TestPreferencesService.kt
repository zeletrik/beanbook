package eu.zeletrik.beanbook

import eu.zeletrik.beanbook.preferences.PreferencesService
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Test stub for PreferencesService. Returns "€" for getCurrency(),
 * stores setCurrency calls in memory. kotlin-spring makes PreferencesService
 * open so this subclass compiles without issues.
 */
class TestPreferencesService : PreferencesService(JdbcTemplate()) {
    private var currency = "€"
    override fun getCurrency() = currency
    override fun setCurrency(symbol: String) { currency = symbol }
}
