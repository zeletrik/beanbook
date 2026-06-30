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
    private val roasterSpendContainer =
        VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.25rem" }
    private val brewSpendContainer =
        VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.4rem" }
    private val brewPaceContainer =
        VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.25rem" }
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
                style["color"] = "var(--lumo-tertiary-text-color)"
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
        contentLayout.add(section("Spend by Brew Method", brewSpendContainer))
        contentLayout.add(
            section("Pace by Brew Method", brewPaceContainer, subtitle = "Avg. days from opening to finishing a bag"),
        )
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
            roasterSpendContainer.removeAll()
            brewSpendContainer.removeAll()
            brewPaceContainer.removeAll()
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

        // Spend by roaster list
        buildSpendList(roasterSpendContainer, analyticsService.totalSpendByRoaster(purchases))

        // Spend by Brew Method — full-width bars proportional to spend, like Origins / Profile.
        val spendByBrew = analyticsService.spendByBrewMethod(purchases)
        val maxBrewSpend = spendByBrew.values.maxOrNull() ?: BigDecimal.ZERO
        brewSpendContainer.removeAll()
        listOf(
            BrewMethod.ESPRESSO to "Espresso",
            BrewMethod.FILTER to "Filter",
            BrewMethod.UNCLASSIFIED to "Unclassified",
        ).forEach { (method, label) ->
            val spend = spendByBrew[method] ?: BigDecimal.ZERO
            brewSpendContainer.add(fullWidthBar(label, spend, maxBrewSpend, spend.formatPrice(getCurrency())))
        }

        // Pace by Brew Method — its own labelled counter (only methods that track open→finish: espresso, filter).
        val paceByBrew = analyticsService.paceByBrewMethod(purchases)
        brewPaceContainer.removeAll()
        listOf(BrewMethod.ESPRESSO to "Espresso", BrewMethod.FILTER to "Filter").forEach { (method, label) ->
            brewPaceContainer.add(paceRow(label, paceByBrew[method]))
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

    /**
     * One row of the "Pace by Brew Method" counter: the method label at the left edge and the average
     * days-to-finish at the right. A null [paceDays] (no opened-and-finished bags for that method yet)
     * shows a muted em dash.
     */
    private fun paceRow(label: String, paceDays: BigDecimal?): HorizontalLayout {
        val labelSpan = Span(label).apply {
            style["font-size"] = "var(--lumo-font-size-m)"; style["flex"] = "1"; style["min-width"] = "0"
            style["overflow"] = "hidden"; style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
        }
        val valueSpan = Span(paceDays?.let { "${it.toInt()} days" } ?: "—").apply {
            style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-m)"; style["white-space"] = "nowrap"
            style["color"] = if (paceDays != null) "var(--lumo-primary-color)" else "var(--lumo-secondary-text-color)"
        }
        return HorizontalLayout(labelSpan, valueSpan).apply {
            isSpacing = true; isPadding = false
            style["width"] = "100%"; style["align-items"] = "center"
            style["justify-content"] = "space-between"; style["padding"] = "0.5rem 0"
            style["border-bottom"] = "1px solid var(--lumo-contrast-5pct)"
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
            style["border"] = "1px solid var(--lumo-contrast-10pct)"
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
            // display:block + width:100% + min-width:0 give the span a width bound inside the column flex
            // so a long value (e.g. a roaster name like "DAK Coffee Roasters") ellipsizes instead of
            // overflowing the tile — without it the span takes its content width and just gets clipped.
            style["display"] = "block"
            style["width"] = "100%"
            style["min-width"] = "0"
            style["overflow"] = "hidden"
            style["text-overflow"] = "ellipsis"
            style["white-space"] = "nowrap"
        }
        return Div().apply {
            style["padding"] = "0.9rem"
            style["border-radius"] = "var(--lumo-border-radius-l)"
            style["background"] = "var(--lumo-base-color)"
            style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
            style["border"] = "1px solid var(--lumo-contrast-10pct)"
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

    /** Integer-valued bar (counts), e.g. origins and profile bag totals. */
    private fun fullWidthBar(label: String, value: Int, max: Int, valueText: String): Div =
        fullWidthBar(label, percentOf(value.toDouble(), max.toDouble()), value > 0, valueText)

    /** Money-valued bar (spend), proportional to [value]/[max]. */
    private fun fullWidthBar(label: String, value: BigDecimal, max: BigDecimal, valueText: String): Div =
        fullWidthBar(label, percentOf(value.toDouble(), max.toDouble()), value.signum() > 0, valueText)

    private fun percentOf(value: Double, max: Double): Int = if (max > 0) (value / max * 100).toInt() else 0

    /**
     * Full-width bar that uses both ends of the card: [label] sits at the left edge, [valueText] at
     * the right edge, and a fill of [fillPct] runs behind them across the row. [nonZero] gives a tiny
     * minimum fill so a present-but-small value is still visible.
     */
    private fun fullWidthBar(label: String, fillPct: Int, nonZero: Boolean, valueText: String): Div {
        val fill = Div().apply {
            style["position"] = "absolute"
            style["left"] = "0"; style["top"] = "0"; style["bottom"] = "0"
            style["width"] = "$fillPct%"; style["min-width"] = if (nonZero) "0.5rem" else "0"
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

    private fun section(title: String, content: VerticalLayout, subtitle: String? = null): VerticalLayout =
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
                style["border"] = "1px solid var(--lumo-contrast-10pct)"
                style["box-sizing"] = "border-box"
                // Shared min-height floor: a sparse section (e.g. a two-item list) gets the same visual
                // weight as the chart instead of collapsing tiny. Centre content so the floored space
                // reads as intentional; taller content (the chart) simply grows past the floor.
                style["min-height"] = "150px"
                style["display"] = "flex"; style["flex-direction"] = "column"; style["justify-content"] = "center"
                // Optional one-line clarifier under the title, inside the card, so an at-a-glance metric
                // like pace explains its unit without a legend.
                subtitle?.let {
                    add(Span(it).apply {
                        style["font-size"] = "var(--lumo-font-size-xs)"; style["color"] = "var(--lumo-secondary-text-color)"
                        style["display"] = "block"; style["margin-bottom"] = "0.5rem"
                    })
                }
                content.width = "100%"
                add(content)
            })
        }
}
