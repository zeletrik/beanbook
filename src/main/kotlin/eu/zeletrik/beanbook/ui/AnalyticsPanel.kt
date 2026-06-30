package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import eu.zeletrik.beanbook.analytics.BrewMethod
import eu.zeletrik.beanbook.analytics.AnalyticsService
import eu.zeletrik.beanbook.analytics.MonthlySpend
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.RoastProfile
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.TextStyle
import java.util.Locale

/**
 * Scrollable panel that summarises a list of [BeanPurchase] into stat cards and charts using
 * [AnalyticsService], or shows an empty state when there are no purchases.
 */
class AnalyticsPanel(
    private val analyticsService: AnalyticsService,
    private val getCurrency: () -> String = { "€" },
) : VerticalLayout() {

    /** Span holding the total-spend headline value; exposed for test assertions. */
    internal val totalCostSpan = Span().also { it.setId("total-cost") }

    private val avgCostValue = Span()
    private val topOriginValue = Span()
    private val priceyBeanValue = Span()
    private val priceyRoasterValue = Span()
    internal val avgPaceValue = Span()
    internal val monthlyCostValue = Span()

    private val spendChartContainer = VerticalLayout().apply { isPadding = false; isSpacing = false; width = "100%" }
    private val originBarsContainer =
        VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.4rem" }
    private val beanSpendContainer =
        VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.25rem" }
    private val roasterSpendContainer =
        VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.25rem" }
    private val brewMethodContainer =
        VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.4rem" }
    private val profileContainer =
        VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.5rem"; width = "100%" }

    // All stats live in contentLayout; emptyState replaces it when there are no purchases.
    private val contentLayout = VerticalLayout().apply { isPadding = false; isSpacing = true; width = "100%" }
    private val emptyState = Div().apply {
        setId("analytics-empty-state")
        style["display"] = "flex"
        style["flex-direction"] = "column"
        style["align-items"] = "center"
        style["justify-content"] = "center"
        // Full width + grow so it actually centres in the panel: the panel is a VerticalLayout
        // (children default to align-items:flex-start), so without width:100% the block hugs the left.
        style["width"] = "100%"
        style["flex"] = "1"
        style["gap"] = "0.75rem"
        style["padding"] = "3rem 1rem"
        style["text-align"] = "center"
        style["box-sizing"] = "border-box"
        style["color"] = "var(--lumo-secondary-text-color)"
        add(
            Icon(VaadinIcon.COFFEE).apply {
                style["width"] = "3rem"; style["height"] = "3rem"
                style["color"] = "var(--lumo-primary-color)"
            },
            Span("Add beans to see your stats").apply {
                style["font-size"] = "var(--lumo-font-size-m)"
            },
        )
        isVisible = false
    }

    init {
        setSizeFull()
        isPadding = true
        isSpacing = true
        style["overflow-y"] = "auto"

        // ── Hero card: Total Spent ─────────────────────────────
        val heroCard = heroCard(
            icon = VaadinIcon.MONEY,
            iconColor = "var(--lumo-success-color)",
            label = "Total Spent",
            valueSpan = totalCostSpan,
        )

        // ── 2-column stat grid ─────────────────────────────────
        val statsGrid = twoColumnGrid()
        statsGrid.add(statCard(VaadinIcon.TRENDING_UP, "var(--lumo-primary-color)", "Avg. Price", avgCostValue))
        statsGrid.add(statCard(VaadinIcon.GLOBE, "#2e7d9c", "Top Origin", topOriginValue))
        statsGrid.add(statCard(VaadinIcon.TROPHY, "#e6a817", "Priciest Bean", priceyBeanValue))
        statsGrid.add(statCard(VaadinIcon.SHOP, "#7c4dff", "Priciest Roaster", priceyRoasterValue))
        statsGrid.add(statCard(VaadinIcon.TIMER, "#2e8b57", "Avg. Pace", avgPaceValue))
        statsGrid.add(statCard(VaadinIcon.CALENDAR, "#c25e00", "Monthly Cost", monthlyCostValue))

        // ── Breakdown sections ─────────────────────────────────
        contentLayout.add(heroCard, statsGrid)
        contentLayout.add(section("Spend over time", spendChartContainer))
        contentLayout.add(section("Origins", originBarsContainer))
        contentLayout.add(section("Spend by Roaster", roasterSpendContainer))
        contentLayout.add(section("Brew Method", brewMethodContainer))
        contentLayout.add(section("Purchased by Profile", profileContainer))
        add(contentLayout, emptyState)
    }

    /** A responsive 2-up grid (used for the stat tiles and the side-by-side spend lists). */
    private fun twoColumnGrid(): Div = Div().apply {
        style["display"] = "grid"
        style["grid-template-columns"] = "repeat(2, minmax(0, 1fr))"
        style["grid-auto-rows"] = "1fr"
        style["gap"] = "0.75rem"
        width = "100%"
    }

    /** Recomputes every stat, list and chart from [purchases], or shows the empty state when it is empty. */
    fun update(purchases: List<BeanPurchase>) {
        emptyState.isVisible = purchases.isEmpty()
        contentLayout.isVisible = purchases.isNotEmpty()
        if (purchases.isEmpty()) {
            totalCostSpan.text = "—"
            avgCostValue.text = "—"
            topOriginValue.text = "—"
            priceyBeanValue.text = "—"
            priceyRoasterValue.text = "—"
            avgPaceValue.text = "—"
            monthlyCostValue.text = "—"
            spendChartContainer.removeAll()
            originBarsContainer.removeAll()
            beanSpendContainer.removeAll()
            roasterSpendContainer.removeAll()
            brewMethodContainer.removeAll()
            profileContainer.removeAll()
            return
        }

        totalCostSpan.text = analyticsService.totalCost(purchases).formatPrice(getCurrency())
        avgCostValue.text = analyticsService.averageCost(purchases).formatPrice(getCurrency())
        topOriginValue.text = analyticsService.mostCommonOrigin(purchases) ?: "—"
        val expBean = analyticsService.mostExpensiveBean(purchases)
        priceyBeanValue.text = expBean?.name ?: "—"
        // Update secondary price in priceyBeanValue's parent (stored sibling span not needed — just set as subtitle)
        priceyRoasterValue.text = analyticsService.mostExpensiveRoaster(purchases) ?: "—"
        avgPaceValue.text = analyticsService.averagePaceDays(purchases)?.let { "${it.toInt()} days" } ?: "—"
        monthlyCostValue.text =
            analyticsService.projectedMonthlyCost(purchases)?.formatPrice(getCurrency())?.let { "$it/mo" } ?: "—"

        // Spend-over-time bar chart
        buildSpendChart(analyticsService.spendByMonth(purchases))

        // Origins — full-width bars (label at the left end, count at the right end)
        val originBreakdown = analyticsService.originBreakdown(purchases)
        val maxOriginCount = originBreakdown.values.maxOrNull() ?: 1
        originBarsContainer.removeAll()
        originBreakdown.entries
            .sortedByDescending { it.value }
            .forEach { (origin, count) ->
                originBarsContainer.add(fullWidthBar(origin, count, maxOriginCount, "$count"))
            }

        // Spend lists (side-by-side row)
        buildSpendList(beanSpendContainer, analyticsService.totalSpendByBean(purchases))
        buildSpendList(roasterSpendContainer, analyticsService.totalSpendByRoaster(purchases))

        // Brew Method section
        brewMethodContainer.removeAll()
        val spendByBrew = analyticsService.spendByBrewMethod(purchases)
        val countByBrew = analyticsService.countByBrewMethod(purchases)
        val paceByBrew = analyticsService.paceByBrewMethod(purchases)

        listOf(
            BrewMethod.ESPRESSO to "Espresso",
            BrewMethod.FILTER to "Filter",
            BrewMethod.UNCLASSIFIED to "Unclassified"
        ).forEach { (method, label) ->
            val spend = spendByBrew[method] ?: BigDecimal.ZERO
            val count = countByBrew[method] ?: 0
            val pace = paceByBrew[method]
            val paceText = if (pace != null) "${pace.toInt()} days" else "—"
            val row = buildBrewMethodRow(
                label,
                spend.formatPrice(getCurrency()),
                count,
                paceText,
                method != BrewMethod.UNCLASSIFIED
            )
            brewMethodContainer.add(row)
        }

        // Purchased by Profile — its own full-width card (bag counts as full-width bars)
        val countByProfile = analyticsService.countByRoastProfile(purchases)
        val maxProfileCount = countByProfile.values.maxOrNull()?.takeIf { it > 0 } ?: 1
        profileContainer.removeAll()
        listOf(
            RoastProfile.ESPRESSO to "Espresso",
            RoastProfile.FILTER to "Filter",
            RoastProfile.OMNI to "Omni"
        ).forEach { (profile, label) ->
            val count = countByProfile[profile] ?: 0
            profileContainer.add(fullWidthBar(label, count, maxProfileCount, "$count bags"))
        }
    }

    private fun buildBrewMethodRow(
        label: String,
        spend: String,
        count: Int,
        pace: String,
        showPace: Boolean
    ): HorizontalLayout {
        val labelSpan = Span(label).apply {
            style["font-size"] = "var(--lumo-font-size-m)"; style["font-weight"] = "600"
        }
        val spendSpan = Span(spend).apply {
            style["font-weight"] = "600"
            style["font-size"] = "var(--lumo-font-size-m)"
            style["color"] = "var(--lumo-primary-color)"
        }
        val countSpan = Span("${count}×").apply {
            style["font-size"] = "var(--lumo-font-size-s)"
            style["color"] = "var(--lumo-secondary-text-color)"
        }
        val paceSpan = Span(if (showPace) pace else "").apply {
            style["font-size"] = "var(--lumo-font-size-s)"
            style["color"] = "var(--lumo-secondary-text-color)"
        }
        // Metrics grouped at the right edge; label at the left edge (uses both ends of the row).
        val metrics = HorizontalLayout(spendSpan, countSpan, paceSpan).apply {
            isPadding = false; isSpacing = true; style["align-items"] = "baseline"
        }
        return HorizontalLayout(labelSpan, metrics).apply {
            isSpacing = true; isPadding = false
            style["width"] = "100%"; style["align-items"] = "center"
            style["justify-content"] = "space-between"; style["padding"] = "0.4rem 0"
        }
    }

    // ── Builders ──────────────────────────────────────────────────

    private fun heroCard(icon: VaadinIcon, iconColor: String, label: String, valueSpan: Span): Div {
        val iconDiv = iconCircle(icon, iconColor, "2.2rem", "3rem")
        val labelSpan = Span(label).apply {
            style["color"] = "var(--lumo-secondary-text-color)"
            style["font-size"] = "var(--lumo-font-size-s)"
        }
        valueSpan.apply {
            style["font-size"] = "var(--lumo-font-size-xxl)"
            style["font-weight"] = "700"
            style["letter-spacing"] = "-0.02em"
        }
        val textCol = VerticalLayout(labelSpan, valueSpan).apply {
            isPadding = false; isSpacing = false; style["gap"] = "0.1rem"
            style["flex"] = "1"
        }
        return Div().apply {
            style["display"] = "flex"
            style["align-items"] = "center"
            style["gap"] = "1rem"
            style["padding"] = "1rem"
            style["border-radius"] = "var(--lumo-border-radius-l)"
            style["background"] = "var(--lumo-base-color)"
            style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
            style["width"] = "100%"
            style["box-sizing"] = "border-box"
            add(iconDiv)
            add(textCol)
        }
    }

    private fun statCard(icon: VaadinIcon, iconColor: String, label: String, valueSpan: Span): Div {
        val iconDiv = iconCircle(icon, iconColor, "1.4rem", "2.2rem")
        val labelSpan = Span(label).apply {
            style["color"] = "var(--lumo-secondary-text-color)"
            style["font-size"] = "var(--lumo-font-size-xs)"
        }
        valueSpan.apply {
            style["font-weight"] = "700"
            style["font-size"] = "var(--lumo-font-size-l)"
            style["overflow"] = "hidden"
            style["text-overflow"] = "ellipsis"
            style["white-space"] = "nowrap"
        }
        return Div().apply {
            style["padding"] = "0.9rem"
            style["border-radius"] = "var(--lumo-border-radius-l)"
            style["background"] = "var(--lumo-base-color)"
            style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
            style["overflow"] = "hidden"
            // Shared min-height + grid-auto-rows:1fr make all six tiles one uniform size; centre the
            // stack so a tile is never top-hugging. height:100% lets the grid equalise rows.
            style["height"] = "100%"
            style["min-height"] = "116px"
            style["min-width"] = "0"
            style["box-sizing"] = "border-box"
            style["display"] = "flex"; style["flex-direction"] = "column"; style["justify-content"] = "center"
            val inner = VerticalLayout(iconDiv, labelSpan, valueSpan).apply {
                isPadding = false; isSpacing = false; style["gap"] = "0.3rem"
                width = "100%"; style["min-width"] = "0"
            }
            add(inner)
        }
    }

    private fun iconCircle(icon: VaadinIcon, color: String, iconSize: String, circleSize: String): Div =
        Div(Icon(icon).apply {
            style["color"] = color
            style["width"] = iconSize; style["height"] = iconSize
        }).apply {
            style["width"] = circleSize; style["height"] = circleSize
            style["border-radius"] = "50%"
            style["background"] = "${color}1a"
            style["display"] = "flex"; style["align-items"] = "center"; style["justify-content"] = "center"
            style["flex-shrink"] = "0"
        }

    private fun buildSpendChart(data: List<MonthlySpend>) {
        spendChartContainer.removeAll()
        if (data.isEmpty()) {
            spendChartContainer.add(Span("No spend yet").apply {
                style["font-size"] = "var(--lumo-font-size-s)"
                style["color"] = "var(--lumo-secondary-text-color)"
            })
            return
        }
        val currency = getCurrency()
        val max = data.maxOf { it.total }
        val maxBarPx = 140
        val columns = data.map { (month, total) ->
            val barPx = if (max > BigDecimal.ZERO) (total.toDouble() / max.toDouble() * maxBarPx).toInt() else 0
            val bar = Div().apply {
                style["width"] = "86%"
                style["height"] = "${barPx}px"
                style["min-height"] = if (total > BigDecimal.ZERO) "4px" else "0"
                style["background"] = "var(--lumo-primary-color)"
                style["border-radius"] = "4px 4px 0 0"
            }
            val barArea = Div(bar).apply {
                style["height"] = "${maxBarPx}px"; style["width"] = "100%"
                style["display"] = "flex"; style["align-items"] = "flex-end"; style["justify-content"] = "center"
            }
            val valueLbl =
                Span(if (total > BigDecimal.ZERO) "$currency${total.setScale(0, RoundingMode.HALF_UP)}" else "").apply {
                    style["font-size"] = "var(--lumo-font-size-xs)"; style["color"] = "var(--lumo-secondary-text-color)"
                }
            val monthLbl = Span(month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())).apply {
                style["font-size"] = "var(--lumo-font-size-xs)"; style["color"] = "var(--lumo-secondary-text-color)"
            }
            VerticalLayout(valueLbl, barArea, monthLbl).apply {
                isPadding = false; isSpacing = false; style["gap"] = "2px"
                style["align-items"] = "center"; style["flex"] = "1"
                // Cap so a sparse chart (1–2 months) shows tidy centred bars instead of one huge bar;
                // with a full 6-month window the columns still shrink to share the width.
                style["max-width"] = "33%"
                // Text alternative for screen readers (the bars convey value by height alone).
                element.setAttribute(
                    "aria-label",
                    "${
                        month.month.getDisplayName(
                            TextStyle.FULL,
                            Locale.getDefault()
                        )
                    } ${month.year}: ${total.formatPrice(currency)}",
                )
            }
        }
        spendChartContainer.add(HorizontalLayout(*columns.toTypedArray()).apply {
            isPadding = false; width = "100%"
            style["align-items"] = "flex-end"; style["justify-content"] = "center"; style["gap"] = "0.25rem"
        })
    }

    /**
     * Full-width bar that uses both ends of the card: [label] sits at the left edge, [valueText] at
     * the right edge, and a fill proportional to [value]/[max] runs behind them across the row.
     */
    private fun fullWidthBar(label: String, value: Int, max: Int, valueText: String): Div {
        val pct = if (max > 0) (value.toDouble() / max * 100).toInt() else 0
        val fill = Div().apply {
            style["position"] = "absolute"
            style["left"] = "0"; style["top"] = "0"; style["bottom"] = "0"
            style["width"] = "$pct%"; style["min-width"] = if (value > 0) "0.5rem" else "0"
            style["background"] = "var(--lumo-primary-color-50pct)"
        }
        val labelSpan = Span(label).apply {
            style["font-size"] = "var(--lumo-font-size-m)"; style["flex"] = "1"; style["min-width"] = "0"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
        }
        val valueSpan = Span(valueText).apply {
            style["font-size"] = "var(--lumo-font-size-m)"; style["font-weight"] = "600"
            style["white-space"] = "nowrap"; style["padding-left"] = "0.5rem"
        }
        val labels = HorizontalLayout(labelSpan, valueSpan).apply {
            isPadding = false; isSpacing = false; width = "100%"
            style["align-items"] = "center"; style["justify-content"] = "space-between"
            style["position"] = "relative"   // paint above the fill
            style["padding"] = "0 0.6rem"; style["box-sizing"] = "border-box"
        }
        return Div(fill, labels).apply {
            style["position"] = "relative"; width = "100%"
            style["min-height"] = "2.5rem"
            style["display"] = "flex"; style["align-items"] = "center"
            style["border-radius"] = "var(--lumo-border-radius-m)"
            style["background"] = "var(--lumo-contrast-5pct)"
            style["overflow"] = "hidden"
        }
    }

    private fun buildSpendList(container: VerticalLayout, data: Map<String, BigDecimal>) {
        container.removeAll()
        data.entries.sortedByDescending { it.value }.forEach { (name, amount) ->
            val row = HorizontalLayout(
                Span(name).apply {
                    style["flex"] = "1"; style["overflow"] = "hidden"
                    style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
                    style["font-size"] = "var(--lumo-font-size-m)"
                },
                Span(amount.formatPrice(getCurrency())).apply {
                    style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"
                    style["color"] = "var(--lumo-primary-color)"; style["white-space"] = "nowrap"
                }
            ).apply {
                isSpacing = true; isPadding = false
                style["width"] = "100%"; style["align-items"] = "center"
                style["padding"] = "0.5rem 0"
                style["border-bottom"] = "1px solid var(--lumo-contrast-5pct)"
            }
            container.add(row)
        }
    }

    private fun section(title: String, content: VerticalLayout): VerticalLayout =
        VerticalLayout().apply {
            isPadding = false; isSpacing = false; style["gap"] = "0.5rem"; width = "100%"
            add(H3(title).apply { style["margin"] = "0.5rem 0 0 0"; style["font-size"] = "var(--lumo-font-size-m)" })
            add(Div().apply {
                // VerticalLayout defaults children to align-items:flex-start, so without an explicit
                // width the card shrinks to its content. Force full width so the section spans the screen.
                style["width"] = "100%"
                style["padding"] = "1rem"
                style["border-radius"] = "var(--lumo-border-radius-l)"
                style["background"] = "var(--lumo-base-color)"
                style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
                style["box-sizing"] = "border-box"
                // Shared min-height floor: a sparse section (e.g. a two-item list) gets the same visual
                // weight as the chart instead of collapsing tiny. Centre content so the floored space
                // reads as intentional; taller content (the chart) simply grows past the floor.
                style["min-height"] = "150px"
                style["display"] = "flex"; style["flex-direction"] = "column"; style["justify-content"] = "center"
                content.width = "100%"
                add(content)
            })
        }
}
