package eu.zeletrik.beanbook

import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.ImportResult
import eu.zeletrik.beanbook.beans.ImportService
import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID

/**
 * No-op ImportService stub for Karibu / unit tests.
 * kotlin-spring makes ImportService open; import() is overridden so the
 * constructor args are never actually used.
 */
class TestImportService : ImportService(
    run {
        val repo = object : TestBeanPurchaseRepository() {}
        BeanPurchaseService(repo, repo)
    },
    object : WishlistService(JdbcTemplate()) {
        override fun findAll() = emptyList<WishlistItem>()
        override fun upsert(item: WishlistItem) {}
        override fun deleteById(id: UUID) {}
    },
    jacksonObjectMapper(),
) {
    override fun import(bytes: ByteArray) = ImportResult(0, 0, 0)
}
