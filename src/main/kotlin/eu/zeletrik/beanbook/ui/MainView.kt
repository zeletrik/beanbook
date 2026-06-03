package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Image
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
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.beans.BagState
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.BeanPurchaseService
import java.math.BigDecimal
import java.util.UUID

private const val ONE_STAR = 1
private const val TWO_STARS = 2
private const val THREE_STARS = 3
private const val FOUR_STARS = 4
private const val FIVE_STARS = 5

// Reachable from both MainView and PurchaseDetailView
internal fun Int?.toStars(): String = when (this) {
    ONE_STAR -> "★☆☆☆☆"; TWO_STARS -> "★★☆☆☆"; THREE_STARS -> "★★★☆☆"
    FOUR_STARS -> "★★★★☆"; FIVE_STARS -> "★★★★★"
    else -> ""
}

@Route("")
class MainView(
    private val beanPurchaseService: BeanPurchaseService,
    private val analyticsService: AnalyticsService,
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
    internal val emptyStateMessage = Paragraph("No purchases recorded yet.").also {
        it.setId("empty-state"); it.style["padding"] = "1rem"
    }

    internal val purchaseCount: Int get() = beanPurchaseService.findAll().size

    // Both fields stored so showDetail/hideDetail can hide them when the detail view is open.
    private lateinit var purchasesScrollArea: Div
    private lateinit var purchasesSearchBar: HorizontalLayout

    // Low-stock warning (AC-7–AC-10 / RULE-7, RULE-8)
    internal val lowStockBanner = Paragraph("⚠️ No sealed bags in reserve — time to reorder!").also {
        it.setId("low-stock-banner")
        it.isVisible = false
        it.style["padding"] = "0.5rem 1rem"
        it.style["background"] = "var(--lumo-warning-color-10pct, #fff3cd)"
        it.style["color"] = "var(--lumo-warning-text-color, #856404)"
        it.style["border-radius"] = "var(--lumo-border-radius-m)"
        it.style["margin"] = "0 0.75rem"
    }

    // Filter + sort state
    private var filterState = FilterState()
    private val filterSortDialog = FilterSortDialog { newState ->
        filterState = newState
        updateFilterButton()
        refreshView()
    }
    private val searchField = TextField().apply {
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

    private val analyticsPanel = AnalyticsPanel(analyticsService)
    // Expose totalCostSpan for test assertions (delegates to panel)
    internal val totalCostSpan: Span get() = analyticsPanel.totalCostSpan

    internal val addFormContent = PurchaseFormContent(
        onSave = { bean: PurchaseFormBean, id: UUID? -> handleFormSave(bean, id); navigateTo(0) }
    )

    // purchaseForm and detailView reference each other via lambdas — break the cycle with lateinit
    internal lateinit var purchaseForm: PurchaseForm
    private lateinit var detailView: PurchaseDetailView

    private val purchasesTab = Tab(Icon(VaadinIcon.LIST), Span("Purchases"))
    private val addTab = Tab(Icon(VaadinIcon.PLUS_CIRCLE_O), Span("Add"))
    private val analyticsTab = Tab(Icon(VaadinIcon.CHART), Span("Analytics"))
    private lateinit var tabs: Tabs
    private lateinit var pages: List<VerticalLayout>

    init {
        // Initialise cross-referencing objects first, in init where ordering is explicit
        purchaseForm = PurchaseForm { bean: PurchaseFormBean, id: UUID? ->
            val updated = beanFromBean(bean, id)
            beanPurchaseService.save(updated)
            refreshView()
            if (detailView.isVisible) detailView.show(updated)
        }
        detailView = PurchaseDetailView(
            onBack = { hideDetail() },
            onEdit = { p: BeanPurchase -> purchaseForm.openForEdit(p) },
            onDelete = { purchase: BeanPurchase -> showDeleteConfirmation(purchase) { hideDetail() } },
            onSave = { updated: BeanPurchase -> beanPurchaseService.save(updated); refreshView() },
            // Duplicate: navigate to Add tab first (tab listener calls openForCreate to reset),
            // then immediately pre-fill with profile fields from source (RULE-11).
            onDuplicate = { purchase: BeanPurchase ->
                navigateTo(1)                          // triggers openForCreate via tab listener
                addFormContent.openWithProfile(purchase) // then overlay profile fields
            },
        )

        setSizeFull()
        isPadding = false
        isSpacing = false

        val purchasesPage = buildPurchasesPage()
        val addPage = buildAddPage()
        val analyticsPage = buildAnalyticsPage()
        pages = listOf(purchasesPage, addPage, analyticsPage)

        addPage.isVisible = false
        analyticsPage.isVisible = false

        val contentArea = VerticalLayout(*pages.toTypedArray()).apply {
            setSizeFull(); isPadding = false; isSpacing = false
            style["padding-bottom"] = "calc(56px + env(safe-area-inset-bottom))"
        }

        tabs = Tabs(purchasesTab, addTab, analyticsTab).apply {
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
            mq.addEventListener('change', (e) => apply(e.matches));
        """.trimIndent())
    }

    internal fun navigateTo(tabIndex: Int) {
        tabs.selectedTab = tabs.children.toList()[tabIndex] as Tab
    }

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
        detailView.show(purchase)
    }

    private fun hideDetail() {
        detailView.isVisible = false
        purchasesSearchBar.isVisible = true
        purchasesScrollArea.isVisible = true
        val purchases = beanPurchaseService.findAll()
        cardsLayout.isVisible = purchases.isNotEmpty()
        emptyStateMessage.isVisible = purchases.isEmpty()
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

    private fun buildCard(purchase: BeanPurchase): HorizontalLayout {
        val thumbnail = buildCardThumbnail(purchase)
        val details = buildCardDetails(purchase)
        return HorizontalLayout(thumbnail, details).apply {
            addClassName("bean-row")
            isSpacing = false; isPadding = true
            style["align-items"] = "center"; style["gap"] = "0.75rem"
            style["border-radius"] = "var(--lumo-border-radius-l)"
            style["background"] = "var(--lumo-base-color)"
            style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
            style["cursor"] = "pointer"; style["width"] = "100%"
            addClickListener { showDetail(purchase) }
        }
    }

    private fun buildCardThumbnail(purchase: BeanPurchase): Div =
        if (purchase.imageData != null) {
            val imgWrapper = Div().apply {
                style["width"] = "72px"; style["min-width"] = "72px"; style["height"] = "72px"
                style["border-radius"] = "var(--lumo-border-radius-m)"; style["overflow"] = "hidden"
                style["flex-shrink"] = "0"
            }
            imgWrapper.add(Image(purchase.imageData, "photo.jpg").apply {
                style["width"] = "72px"; style["height"] = "72px"; style["object-fit"] = "cover"
                style["display"] = "block"
            })
            imgWrapper
        } else {
            Div(Span("☕")).apply {
                style["width"] = "72px"; style["min-width"] = "72px"; style["height"] = "72px"
                style["border-radius"] = "var(--lumo-border-radius-m)"
                style["background"] = "var(--lumo-contrast-5pct)"
                style["display"] = "flex"; style["align-items"] = "center"
                style["justify-content"] = "center"; style["font-size"] = "2rem"
                style["flex-shrink"] = "0"
            }
        }

    private fun buildCardDetails(purchase: BeanPurchase): VerticalLayout {
        val (stateLabel, stateColor) = when (purchase.bagState) {
            BagState.SEALED   -> "Sealed"   to "var(--lumo-contrast-60pct)"
            BagState.OPEN     -> "Open"     to "var(--lumo-success-color)"
            BagState.FINISHED -> "Finished" to "var(--lumo-primary-color)"
        }
        val stateBadge = Span(stateLabel).apply {
            style["background"] = "${stateColor}22"; style["color"] = stateColor
            style["border-radius"] = "var(--lumo-border-radius-m)"; style["padding"] = "0.1rem 0.5rem"
            style["font-size"] = "var(--lumo-font-size-xs)"; style["font-weight"] = "600"
        }
        val bottomRow = HorizontalLayout().apply {
            isSpacing = true; isPadding = false
            style["align-items"] = "center"; style["flex-wrap"] = "wrap"; style["gap"] = "0.3rem"
        }
        val ratingText = purchase.rating.toStars()
        if (ratingText.isNotEmpty()) {
            bottomRow.add(Span(ratingText).apply {
                style["font-size"] = "0.85rem"; style["letter-spacing"] = "0.04rem"
            })
        }
        bottomRow.add(stateBadge)
        return VerticalLayout().apply {
            isPadding = false; isSpacing = false
            style["gap"] = "0.15rem"; style["flex"] = "1"; style["overflow"] = "hidden"
            style["min-width"] = "0"
            add(Span(purchase.name).apply {
                style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"
                style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
            })
            add(Span("${purchase.roaster}  ·  ${purchase.origin}").apply {
                style["color"] = "var(--lumo-secondary-text-color)"; style["font-size"] = "var(--lumo-font-size-s)"
                style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
            })
            add(Span("${purchase.pricePerUnit.formatPrice()}  ·  ${purchase.weightGrams} g").apply {
                style["font-size"] = "var(--lumo-font-size-s)"; style["color"] = "var(--lumo-secondary-text-color)"
            })
            add(bottomRow)
        }
    }

    internal fun showDeleteConfirmation(purchase: BeanPurchase, onConfirmed: () -> Unit = {}): Dialog {
        val dialog = Dialog()
        dialog.setId("delete-confirm-dialog")
        dialog.add(Paragraph("Delete '${purchase.name}'?"))
        val confirmBtn = Button("Confirm") {
            beanPurchaseService.delete(purchase.id); refreshView(); onConfirmed(); dialog.close()
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
            pricePerUnit = bean.pricePerUnit!!, weightGrams = bean.weightGrams!!,
            purchaseDate = bean.purchaseDate!!, roastDate = bean.roastDate!!,
            roastLevel = bean.roastLevel!!, process = bean.process!!,
            notes = bean.notes.ifBlank { null },
            grindSettings = bean.grindSettings.trim().takeIf { it.isNotBlank() },
            imageData = bean.imageData,
            rating = bean.rating,
            openedDate = bean.openedDate,
            finishedDate = bean.finishedDate,
        )

    private fun handleFormSave(bean: PurchaseFormBean, existingId: UUID?) {
        beanPurchaseService.save(beanFromBean(bean, existingId))
        refreshView()
    }

    internal fun refreshView() {
        val all = beanPurchaseService.findAll()
        val purchases = all.applyFilter(filterState)
        // Low-stock banner: shown only when list is non-empty AND zero sealed bags remain (RULE-7, RULE-8)
        lowStockBanner.isVisible = !detailView.isVisible && all.isNotEmpty() && all.none { it.bagState == BagState.SEALED }
        cardsLayout.removeAll()
        if (!detailView.isVisible) {
            when {
                all.isEmpty() -> {
                    emptyStateMessage.text = "No purchases recorded yet."
                    emptyStateMessage.isVisible = true
                    cardsLayout.isVisible = false
                }
                purchases.isEmpty() -> {
                    emptyStateMessage.text = "No beans match your search or filters."
                    emptyStateMessage.isVisible = true
                    cardsLayout.isVisible = false
                }
                else -> {
                    emptyStateMessage.isVisible = false
                    cardsLayout.isVisible = true
                    purchases.forEach { cardsLayout.add(buildCard(it)) }
                }
            }
        } else {
            emptyStateMessage.isVisible = false
            cardsLayout.isVisible = false
            purchases.forEach { cardsLayout.add(buildCard(it)) }
        }
        analyticsPanel.update(purchases)
    }
}
