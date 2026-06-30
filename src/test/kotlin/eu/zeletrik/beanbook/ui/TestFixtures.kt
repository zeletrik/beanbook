package eu.zeletrik.beanbook.ui

import eu.zeletrik.beanbook.TestBeanPurchaseRepository
import eu.zeletrik.beanbook.TestImportService
import eu.zeletrik.beanbook.TestPreferencesService
import eu.zeletrik.beanbook.TestWishlistService
import eu.zeletrik.beanbook.ai.AiExtractionService
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.backup.ExportService
import eu.zeletrik.beanbook.backup.ImportService
import eu.zeletrik.beanbook.security.SecurityProperties
import com.vaadin.flow.spring.security.AuthenticationContext
import org.springframework.boot.info.BuildProperties
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * Builds a fully-wired [MainView] for UI tests from in-memory doubles. A single
 * [TestWishlistService] backs both the export service and the view, so wishlist reads and writes
 * stay consistent (the previous per-test inline doubles overrode only findAll()).
 */
internal fun testMainView(
    repo: TestBeanPurchaseRepository,
    wishlist: TestWishlistService = TestWishlistService(),
    importService: ImportService = TestImportService(),
    prefs: TestPreferencesService = TestPreferencesService(),
    aiExtractionService: AiExtractionService? = null,
    securityProperties: SecurityProperties? = null,
    authenticationContext: AuthenticationContext? = null,
    buildProperties: BuildProperties? = null,
): MainView {
    val service = BeanPurchaseService(repo)
    val exportService = ExportService(service, wishlist, jacksonObjectMapper())
    return MainView(
        service, AnalyticsService(), exportService, importService, prefs, wishlist,
        aiExtractionService, securityProperties, authenticationContext, buildProperties,
    )
}

/** Convenience: an in-memory repository pre-seeded with [items]. */
internal fun testRepository(items: List<BeanPurchase> = emptyList()): TestBeanPurchaseRepository =
    object : TestBeanPurchaseRepository() { init { store.addAll(items) } }
