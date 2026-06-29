package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.component.tabs.TabsVariant
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.Route
import com.vaadin.flow.spring.security.AuthenticationContext
import eu.zeletrik.beanbook.ai.AiExtractionService
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.backup.ExportService
import eu.zeletrik.beanbook.backup.ImportService
import eu.zeletrik.beanbook.beans.BagState
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import eu.zeletrik.beanbook.beans.RoastProfile
import eu.zeletrik.beanbook.preferences.PreferencesService
import eu.zeletrik.beanbook.security.SecurityProperties
import eu.zeletrik.beanbook.wishlist.WishlistService
import jakarta.annotation.security.PermitAll
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger(MainView::class.java)

/** Root view of the app: a tabbed bottom-nav shell hosting the purchases, analytics, add, wishlist and settings pages. */
@Route("")
@PermitAll // honoured only when auth is enabled (Vaadin navigation access control); a no-op when open
class MainView(
    private val beanPurchaseService: BeanPurchaseService,
    private val analyticsService: AnalyticsService,
    private val exportService: ExportService,
    private val importService: ImportService,
    private val preferencesService: PreferencesService,
    private val wishlistService: WishlistService,
    // Nullable (Kotlin treats it as a non-required dependency): present only when the AI feature is
    // enabled, so the Add/Edit forms gain "Auto-fill from photo"; absent → the action is hidden.
    private val aiExtractionService: AiExtractionService? = null,
    // Drives whether Settings surfaces the Security section (passkeys + logout). Nullable so Karibu
    // tests (no Spring context) can construct the view directly; absent → treated as auth disabled.
    private val securityProperties: SecurityProperties? = null,
    // Vaadin's logout helper; present when security is on the classpath. Nullable for Karibu tests.
    private val authenticationContext: AuthenticationContext? = null,
) : VerticalLayout() {

    internal val cardsLayout = Div().apply {
        setId("cards-layout")
        style["display"] = "flex"
        style["flex-direction"] = "column"
        style["gap"] = "0.5rem"
        style["padding"] = "0.75rem"
        style["width"] = "100%"
        style["box-sizing"] = "border-box"
    }
    internal val emptyStateMessage = Div().apply {
        setId("empty-state")
        style["flex"] = "1"
        style["width"] = "100%"
        style["display"] = "flex"
        style["flex-direction"] = "column"
        style["align-items"] = "center"
        style["justify-content"] = "center"
        style["gap"] = "0.75rem"
        style["padding"] = "2rem 1rem"
        style["text-align"] = "center"
        style["box-sizing"] = "border-box"
    }

    internal val purchaseCount: Int get() = beanPurchaseService.findAll().size

    /** Stored so [showDetail]/[hideDetail] can hide it when the detail view is open. */
    private lateinit var purchasesScrollArea: Div

    /** Stored so [showDetail]/[hideDetail] can hide it when the detail view is open. */
    private lateinit var purchasesSearchBar: HorizontalLayout

    /** Low-stock warning (AC-7–AC-10 / RULE-7, RULE-8). */
    internal val lowStockBanner = HorizontalLayout(
        Icon(VaadinIcon.WARNING).apply { setSize("var(--lumo-icon-size-s)"); element.setAttribute("aria-hidden", "true") },
        Span("No sealed bags in reserve — time to reorder!"),
    ).also {
        it.setId("low-stock-banner")
        it.isVisible = false
        it.isPadding = false
        it.isSpacing = false
        it.style["align-items"] = "center"
        it.style["gap"] = "0.5rem"
        it.style["padding"] = "0.5rem 1rem"
        it.style["background"] = "var(--lumo-warning-color-10pct, #fff3cd)"
        it.style["color"] = "var(--lumo-warning-text-color, #856404)"
        it.style["border-radius"] = "var(--lumo-border-radius-m)"
        it.style["margin"] = "0 0.75rem"
    }

    /** Current filter + sort state driving the purchases list. */
    internal var filterState = FilterState()
    internal val filterSortDialog = FilterSortDialog(
        onApply = { newState ->
            filterState = newState
            updateFilterButton()
            refreshView()
        },
        getAvailableTags = { beanPurchaseService.allTags() },
    )
    internal val searchField = TextField().apply {
        placeholder = "Search name, roaster, origin…"
        prefixComponent = Icon(VaadinIcon.SEARCH)
        isClearButtonVisible = true
        setValueChangeMode(ValueChangeMode.LAZY)
        addValueChangeListener { event ->
            filterState = filterState.copy(query = event.value)
            refreshView()
        }
        style["flex"] = "1"
    }
    private val filterButton = Button("Filter & Sort") {
        filterSortDialog.openWith(filterState)
    }.apply {
        prefixComponent = Icon(VaadinIcon.FILTER)
        addThemeVariants(ButtonVariant.LUMO_TERTIARY)
        style["white-space"] = "nowrap"
    }

    private val analyticsPanel = AnalyticsPanel(analyticsService, preferencesService::getCurrency)

    /** Exposed for test assertions; delegates to [analyticsPanel]. */
    internal val totalCostSpan: Span get() = analyticsPanel.totalCostSpan

    internal val addFormContent = PurchaseFormContent(
        onSave = { bean: PurchaseFormBean, id: UUID? -> handleFormSave(bean, id); navigateTo(AppTab.PURCHASES) },
        getAllTags = { beanPurchaseService.allTags() },
        getAllRoasters = { beanPurchaseService.allRoasters() },
        aiExtractionService = aiExtractionService,
    )

    /** Edit/create form; references [detailView] via lambdas, so the cycle is broken by initialising it in [init]. */
    internal lateinit var purchaseForm: PurchaseForm

    /** Detail view; references [purchaseForm] via lambdas, so the cycle is broken by initialising it in [init]. */
    internal lateinit var detailView: PurchaseDetailView

    private val purchasesTab = navTab(VaadinIcon.LIST, "Beans")
    private val addTab = navTab(VaadinIcon.PLUS_CIRCLE_O, "Add")
    private val analyticsTab = navTab(VaadinIcon.CHART, "Stats")
    private val settingsTab = navTab(VaadinIcon.COG, "Settings")
    private val wishlistTab = navTab(VaadinIcon.HEART, "Wishlist")
    internal lateinit var tabs: Tabs
    private lateinit var pages: List<VerticalLayout>
    internal lateinit var settingsPage: VerticalLayout

    init {
        // Initialise cross-referencing objects first, in init where ordering is explicit
        purchaseForm = PurchaseForm(
            onSave = { bean: PurchaseFormBean, id: UUID? ->
                val updated = beanFromBean(bean, id)
                try {
                    beanPurchaseService.save(updated)
                    NotificationHelper.success("Bean saved")
                    refreshView()
                    if (detailView.isVisible) detailView.show(updated)
                } catch (e: Exception) {
                    log.error("Failed to save bean {}", updated.id, e)
                    NotificationHelper.error("Failed to save — please try again")
                }
            },
            getAllTags = { beanPurchaseService.allTags() },
            getAllRoasters = { beanPurchaseService.allRoasters() },
            aiExtractionService = aiExtractionService,
        )
        detailView = PurchaseDetailView(
            onBack = { hideDetail() },
            onEdit = { p: BeanPurchase -> purchaseForm.openForEdit(p) },
            onDelete = { purchase: BeanPurchase -> showDeleteConfirmation(purchase) { hideDetail() } },
            // Guarded inline (not a method) so a persistence failure from a detail-view inline edit
            // can't escape into the UI thread — mirrors handleFormSave without the success toast.
            onSave = { updated: BeanPurchase ->
                try {
                    beanPurchaseService.save(updated)
                    refreshView()
                } catch (e: Exception) {
                    log.error("Failed to save bean {}", updated.id, e)
                    NotificationHelper.error("Failed to save — please try again")
                }
            },
            getCurrency = preferencesService::getCurrency,
            // Duplicate: navigate to Add tab first (tab listener calls openForCreate to reset),
            // then immediately pre-fill with profile fields from source (RULE-11).
            onDuplicate = { purchase: BeanPurchase ->
                navigateTo(AppTab.ADD)                   // triggers openForCreate via tab listener
                addFormContent.openWithProfile(purchase) // then overlay profile fields
            },
        )

        setSizeFull()
        isPadding = false
        isSpacing = false

        val purchasesPage = buildPurchasesPage()
        val addPage = buildAddPage()
        val analyticsPage = buildAnalyticsPage()
        val wishlistPage = buildWishlistPage()  // must come before buildSettingsPage so wishlistView is initialized
        settingsPage = buildSettingsPage()
        pages = listOf(purchasesPage, analyticsPage, addPage, wishlistPage, settingsPage)

        addPage.isVisible = false
        analyticsPage.isVisible = false
        settingsPage.isVisible = false
        wishlistPage.isVisible = false

        val contentArea = VerticalLayout(*pages.toTypedArray()).apply {
            setSizeFull(); isPadding = false; isSpacing = false
            style["padding-bottom"] = "calc(56px + env(safe-area-inset-bottom))"
        }

        tabs = Tabs(purchasesTab, analyticsTab, addTab, wishlistTab, settingsTab).apply {
            addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS)
            style["position"] = "fixed"; style["bottom"] = "0"
            style["left"] = "0"; style["right"] = "0"; style["width"] = "100%"
            style["min-height"] = "56px"
            style["height"] = "auto"
            style["padding-bottom"] = "env(safe-area-inset-bottom)"
            style["z-index"] = "100"
            style["border-top"] = "1px solid var(--lumo-contrast-10pct)"
            style["background"] = "var(--lumo-base-color)"
        }
        tabs.addSelectedChangeListener { event ->
            val tabList = tabs.children.toList()
            pages.forEachIndexed { i, page -> page.isVisible = tabList.indexOf(event.selectedTab) == i }
            if (event.selectedTab == addTab) addFormContent.openForCreate()
        }

        add(contentArea, tabs)
        setFlexGrow(1.0, contentArea)

        addFormContent.openForCreate()
        add(purchaseForm, filterSortDialog)
        refreshView()
        applySystemTheme()
    }

    private fun applySystemTheme() {
        UI.getCurrent()?.page?.executeJs("""
            document.documentElement.style.height = '100dvh';
            document.body.style.height = '100dvh';
            const apply = (dark) => { document.documentElement.setAttribute('theme', dark ? 'dark' : ''); };
            const mq = window.matchMedia('(prefers-color-scheme: dark)');
            apply(mq.matches);
            // Bind the change listener once per page — MainView is recreated on navigation, and
            // re-running this without the guard would leak a new listener each time.
            if (!window.__beanbookThemeBound) {
                window.__beanbookThemeBound = true;
                mq.addEventListener('change', (e) => apply(e.matches));
            }
        """.trimIndent())
    }

    internal fun navigateTo(tab: AppTab) {
        tabs.selectedTab = tabFor(tab)
    }

    private fun tabFor(tab: AppTab): Tab = when (tab) {
        AppTab.PURCHASES -> purchasesTab
        AppTab.ANALYTICS -> analyticsTab
        AppTab.ADD -> addTab
        AppTab.WISHLIST -> wishlistTab
        AppTab.SETTINGS -> settingsTab
    }

    /**
     * Builds an icon + caption bottom-nav [Tab]. The caption aids discoverability; the aria-label gives
     * screen readers a name for what would otherwise be an icon-only control.
     */
    private fun navTab(icon: VaadinIcon, label: String): Tab =
        Tab(VerticalLayout(
            Icon(icon),
            Span(label).apply {
                style["font-size"] = "var(--lumo-font-size-xxs)"
                style["line-height"] = "1"
            },
        ).apply {
            isPadding = false; isSpacing = false
            style["align-items"] = "center"
            style["gap"] = "2px"
        }).apply { element.setAttribute("aria-label", label) }

    private fun updateFilterButton() {
        val count = filterState.activeFilterCount
        filterButton.text = if (count > 0) "Filter & Sort ($count)" else "Filter & Sort"
        if (count > 0) filterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        else filterButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY)
    }

    private fun showDetail(purchase: BeanPurchase) {
        purchasesSearchBar.isVisible = false
        purchasesScrollArea.isVisible = false
        emptyStateMessage.isVisible = false
        // The low-stock banner is a list-level reorder nudge; on the detail page its extra height pushed
        // the action buttons under the fixed bottom nav (#22). hideDetail() restores it via refreshView().
        lowStockBanner.isVisible = false
        detailView.show(purchase)
    }

    private fun hideDetail() {
        detailView.isVisible = false
        purchasesSearchBar.isVisible = true
        // Delegate to refreshView so cards + empty-state are rebuilt against the active filter,
        // rather than re-deriving visibility from the unfiltered list without rebuilding cards.
        refreshView()
    }

    private fun buildPurchasesPage(): VerticalLayout {
        purchasesSearchBar = HorizontalLayout(searchField, filterButton).apply {
            isSpacing = true; isPadding = true
            style["width"] = "100%"; style["box-sizing"] = "border-box"
            style["border-bottom"] = "1px solid var(--lumo-contrast-10pct)"
            style["flex-shrink"] = "0"
            setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER)
        }
        purchasesScrollArea = Div(cardsLayout).apply {
            setSizeFull(); style["overflow-y"] = "auto"; style["flex"] = "1"
        }
        return VerticalLayout(purchasesSearchBar, lowStockBanner, purchasesScrollArea, emptyStateMessage, detailView).apply {
            setSizeFull(); isPadding = false; isSpacing = false
            setFlexGrow(1.0, purchasesScrollArea)
            setFlexGrow(1.0, detailView)
        }
    }

    private fun buildAddPage(): VerticalLayout {
        val scrollable = VerticalLayout(H2("New Purchase"), addFormContent).apply {
            isPadding = true; isSpacing = true; width = "100%"
        }
        return VerticalLayout(scrollable).apply {
            setSizeFull(); isPadding = false; isSpacing = false
            style["overflow-y"] = "auto"
        }
    }

    private fun buildAnalyticsPage(): VerticalLayout =
        VerticalLayout(H2("Analytics"), analyticsPanel).apply {
            setSizeFull(); isPadding = true; isSpacing = false
            setFlexGrow(1.0, analyticsPanel)
        }

    private fun buildSettingsPage(): VerticalLayout {
        val settingsView = SettingsView(
            exportService, importService, preferencesService,
            onImportComplete = {
                refreshView()
                wishlistView.refreshList()
            },
            // refreshView() re-renders the list cards and the analytics panel with the new symbol.
            onCurrencyChanged = { refreshView() },
            securityEnabled = securityProperties?.enabled == true,
            onLogout = { authenticationContext?.logout() },
        )
        val scrollable = VerticalLayout(H2("Settings"), settingsView).apply {
            isPadding = true; isSpacing = true; width = "100%"
        }
        return VerticalLayout(scrollable).apply {
            setSizeFull(); isPadding = false; isSpacing = false
            style["overflow-y"] = "auto"
        }
    }

    private lateinit var wishlistView: WishlistView

    private fun buildWishlistPage(): VerticalLayout {
        wishlistView = WishlistView(wishlistService)
        return VerticalLayout(wishlistView).apply {
            setSizeFull(); isPadding = false; isSpacing = false
        }
    }


    internal fun showDeleteConfirmation(purchase: BeanPurchase, onConfirmed: () -> Unit = {}): Dialog {
        val dialog = Dialog()
        dialog.setId("delete-confirm-dialog")
        dialog.add(Paragraph("Delete '${purchase.name}'?"))
        val confirmBtn = Button("Confirm") {
            try {
                beanPurchaseService.delete(purchase.id)
                NotificationHelper.successWithUndo("Bean deleted") {
                    beanPurchaseService.save(purchase)
                    refreshView()
                }
                refreshView()
                onConfirmed()
                dialog.close()
            } catch (e: Exception) {
                log.error("Failed to delete bean {}", purchase.id, e)
                NotificationHelper.error("Failed to delete — please try again")
            }
        }.apply { setId("confirm-delete-btn"); addThemeVariants(ButtonVariant.LUMO_ERROR) }
        val cancelBtn = Button("Cancel") { dialog.close() }.apply { setId("cancel-delete-btn") }
        dialog.add(HorizontalLayout(confirmBtn, cancelBtn))
        dialog.open()
        return dialog
    }

    private fun beanFromBean(bean: PurchaseFormBean, existingId: UUID?): BeanPurchase =
        BeanPurchase(
            id = existingId ?: UUID.randomUUID(),
            name = bean.name, roaster = bean.roaster, origin = bean.origin,
            price = bean.price!!, weightGrams = bean.weightGrams!!,
            purchaseDate = bean.purchaseDate!!, roastDate = bean.roastDate!!,
            roastLevel = bean.roastLevel!!, process = bean.process!!,
            notes = bean.notes.ifBlank { null },
            grindSettings = bean.grindSettings.trim().takeIf { it.isNotBlank() },
            imageData = bean.imageData,
            rating = bean.rating,
            openedDate = bean.openedDate,
            finishedDate = bean.finishedDate,
            roastProfile = bean.roastProfile ?: RoastProfile.OMNI,
            usedAs = if ((bean.roastProfile ?: RoastProfile.OMNI) == RoastProfile.OMNI) bean.usedAs else null,
            tags = bean.tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet(),
            url = bean.url.toHref(),
            region = bean.region.trim().takeIf { it.isNotBlank() },
        )

    private fun handleFormSave(bean: PurchaseFormBean, existingId: UUID?) {
        try {
            beanPurchaseService.save(beanFromBean(bean, existingId))
            NotificationHelper.success("Bean saved")
            refreshView()
        } catch (e: Exception) {
            log.error("Failed to save bean", e)
            NotificationHelper.error("Failed to save — please try again")
        }
    }

    internal fun refreshView() {
        val all = beanPurchaseService.findAll()
        val purchases = all.applyFilter(filterState)
        analyticsPanel.update(purchases)
        // Low-stock banner: shown only when list is non-empty AND zero sealed bags remain (RULE-7, RULE-8)
        lowStockBanner.isVisible = !detailView.isVisible && all.isNotEmpty() && all.none { it.bagState == BagState.SEALED }

        // The detail view covers the list; its page is rebuilt when it closes (hideDetail → refreshView),
        // so there's no point rendering cards into a hidden layout here.
        if (detailView.isVisible) return

        cardsLayout.removeAll()
        when {
            all.isEmpty() -> {
                showFirstUseEmptyState()
                emptyStateMessage.isVisible = true
                purchasesScrollArea.isVisible = false
                cardsLayout.isVisible = false
            }
            purchases.isEmpty() -> {
                showNoResultsEmptyState()
                emptyStateMessage.isVisible = true
                purchasesScrollArea.isVisible = false
                cardsLayout.isVisible = false
            }
            else -> {
                val currency = preferencesService.getCurrency()
                emptyStateMessage.isVisible = false
                purchasesScrollArea.isVisible = true
                cardsLayout.isVisible = true
                purchases.forEach { p -> cardsLayout.add(beanCard(p, currency) { showDetail(p) }) }
            }
        }
    }

    private fun showFirstUseEmptyState() {
        emptyStateMessage.removeAll()
        emptyStateMessage.add(Icon(VaadinIcon.COFFEE).apply {
            setSize("3rem"); style["color"] = "var(--lumo-tertiary-text-color)"
            element.setAttribute("aria-hidden", "true")
        })
        emptyStateMessage.add(Span("No beans yet").apply {
            style["font-weight"] = "700"
            style["font-size"] = "var(--lumo-font-size-xl)"
        })
        emptyStateMessage.add(Span("Start tracking your coffee journey").apply {
            style["color"] = "var(--lumo-secondary-text-color)"
            style["font-size"] = "var(--lumo-font-size-s)"
        })
        emptyStateMessage.add(
            Button("Add your first bean") { navigateTo(AppTab.ADD) }.apply {
                setId("empty-state-cta")
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                style["margin-top"] = "0.5rem"
            }
        )
    }

    private fun showNoResultsEmptyState() {
        emptyStateMessage.removeAll()
        emptyStateMessage.add(Span("🔍").apply { style["font-size"] = "3rem" })
        emptyStateMessage.add(Span("No beans match your search or filters").apply {
            style["color"] = "var(--lumo-secondary-text-color)"
            style["font-size"] = "var(--lumo-font-size-s)"
        })
    }
}
