package eu.zeletrik.beanbook

import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.backup.ImportResult
import eu.zeletrik.beanbook.backup.ImportService
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
        BeanPurchaseService(repo)
    },
    object : WishlistService(TestWishlistRepository()) {
        override fun findAll() = emptyList<WishlistItem>()
        override fun upsert(item: WishlistItem) = Unit
        override fun deleteById(id: UUID) = Unit
    },
    jacksonObjectMapper(),
) {
    override fun import(bytes: ByteArray) = ImportResult(0, 0, 0)
}
