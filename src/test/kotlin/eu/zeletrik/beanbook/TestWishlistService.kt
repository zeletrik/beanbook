package eu.zeletrik.beanbook

import eu.zeletrik.beanbook.wishlist.WishlistItem
import eu.zeletrik.beanbook.wishlist.WishlistService

/**
 * The real [WishlistService] backed by an in-memory [TestWishlistRepository] — so tests exercise the
 * actual service logic. [store] exposes the backing list for direct seeding/inspection.
 */
class TestWishlistService private constructor(
    private val repo: TestWishlistRepository,
) : WishlistService(repo) {
    constructor() : this(TestWishlistRepository())

    val store: MutableList<WishlistItem> get() = repo.store
}
