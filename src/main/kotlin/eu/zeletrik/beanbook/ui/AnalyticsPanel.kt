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
import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.RoastProfile
import java.math.BigDecimal

class AnalyticsPanel(
    private val analyticsService: AnalyticsService,
    private val getCurrency: () -> String = { "€" },
) : VerticalLayout() {

    // Exposed for test assertions
    internal val totalCostSpan = Span().also { it.setId("total-cost") }

    private val avgCostValue = Span()
    private val topOriginValue = Span()
    private val priceyBeanValue = Span()
    private val priceyRoasterValue = Span()
    internal val avgPaceValue = Span()
    internal val monthlyCostValue = Span()

    private val originBarsContainer = VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.4rem" }
    private val beanSpendContainer = VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.25rem" }
    private val roasterSpendContainer = VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.25rem" }
    private val brewMethodContainer = VerticalLayout().apply { isPadding = false; isSpacing = false; style["gap"] = "0.4rem" }

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
        val statsGrid = Div().apply {
            style["display"] = "grid"
            style["grid-template-columns"] = "1fr 1fr"
            style["gap"] = "0.75rem"
            width = "100%"
        }
        statsGrid.add(statCard(VaadinIcon.TRENDING_UP, "var(--lumo-primary-color)", "Avg. Price", avgCostValue))
        statsGrid.add(statCard(VaadinIcon.GLOBE, "#2e7d9c", "Top Origin", topOriginValue))
        statsGrid.add(
            statCard(VaadinIcon.TROPHY, "#e6a817", "Priciest Bean", priceyBeanValue).also { card ->
                // append secondary price line inside the card's inner layout
                card.element.children.findFirst().ifPresent { inner ->
                    // priceyBeanPrice is added below priceyBeanValue inside the card
                }
            }
        )
        statsGrid.add(statCard(VaadinIcon.SHOP, "#7c4dff", "Top Roaster", priceyRoasterValue))
        statsGrid.add(statCard(VaadinIcon.TIMER, "#2e8b57", "Avg. Pace", avgPaceValue))
        statsGrid.add(statCard(VaadinIcon.CALENDAR, "#c25e00", "Monthly Cost", monthlyCostValue))

        // ── Breakdown sections ─────────────────────────────────
        add(heroCard, statsGrid)
        add(section("Origins", originBarsContainer))
        add(section("Spend by Bean", beanSpendContainer))
        add(section("Spend by Roaster", roasterSpendContainer))
        add(section("Brew Method", brewMethodContainer))
    }

    fun update(purchases: List<BeanPurchase>) {
        if (purchases.isEmpty()) {
            totalCostSpan.text = "—"
            avgCostValue.text = "—"
            topOriginValue.text = "—"
            priceyBeanValue.text = "—"
            priceyRoasterValue.text = "—"
            avgPaceValue.text = "—"
            monthlyCostValue.text = "—"
            originBarsContainer.removeAll()
            beanSpendContainer.removeAll()
            roasterSpendContainer.removeAll()
            brewMethodContainer.removeAll()
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
        monthlyCostValue.text = analyticsService.projectedMonthlyCost(purchases)?.formatPrice(getCurrency())?.let { "$it/mo" } ?: "—"

        // Origin bars
        val originBreakdown = analyticsService.originBreakdown(purchases)
        val maxCount = originBreakdown.values.maxOrNull() ?: 1
        originBarsContainer.removeAll()
        originBreakdown.entries
            .sortedByDescending { it.value }
            .forEach { (origin, count) ->
                originBarsContainer.add(buildOriginBar(origin, count, maxCount))
            }

        // Spend lists
        buildSpendList(beanSpendContainer, analyticsService.totalSpendByBean(purchases))
        buildSpendList(roasterSpendContainer, analyticsService.totalSpendByRoaster(purchases))

        // Brew Method section
        brewMethodContainer.removeAll()
        val spendByBrew  = analyticsService.spendByBrewMethod(purchases)
        val countByBrew  = analyticsService.countByBrewMethod(purchases)
        val paceByBrew   = analyticsService.paceByBrewMethod(purchases)
        val countByProfile = analyticsService.countByRoastProfile(purchases)

        listOf(BrewMethod.ESPRESSO to "Espresso", BrewMethod.FILTER to "Filter", BrewMethod.UNCLASSIFIED to "Unclassified").forEach { (method, label) ->
            val spend = spendByBrew[method] ?: BigDecimal.ZERO
            val count = countByBrew[method] ?: 0
            val pace  = paceByBrew[method]
            val paceText = if (pace != null) "${pace.toInt()} days" else "—"
            val row = buildBrewMethodRow(label, spend.formatPrice(getCurrency()), count, paceText, method != BrewMethod.UNCLASSIFIED)
            brewMethodContainer.add(row)
        }

        // Raw roast profile distribution
        brewMethodContainer.add(Span("Purchased by profile").apply {
            style["font-size"] = "var(--lumo-font-size-xs)"
            style["color"] = "var(--lumo-secondary-text-color)"
            style["margin-top"] = "0.5rem"
        })
        listOf(RoastProfile.ESPRESSO to "Espresso", RoastProfile.FILTER to "Filter", RoastProfile.OMNI to "Omni").forEach { (profile, label) ->
            val count = countByProfile[profile] ?: 0
            brewMethodContainer.add(buildProfileCountRow("$label: $count bags"))
        }
    }

    private fun buildBrewMethodRow(label: String, spend: String, count: Int, pace: String, showPace: Boolean): HorizontalLayout {
        val labelSpan = Span(label).apply {
            style["font-size"] = "var(--lumo-font-size-s)"
            style["min-width"] = "90px"
        }
        val spendSpan = Span(spend).apply {
            style["font-weight"] = "600"
            style["font-size"] = "var(--lumo-font-size-s)"
            style["color"] = "var(--lumo-primary-color)"
        }
        val countSpan = Span("${count}×").apply {
            style["font-size"] = "var(--lumo-font-size-xs)"
            style["color"] = "var(--lumo-secondary-text-color)"
            style["min-width"] = "30px"
        }
        val paceSpan = Span(if (showPace) pace else "").apply {
            style["font-size"] = "var(--lumo-font-size-xs)"
            style["color"] = "var(--lumo-secondary-text-color)"
            style["min-width"] = "60px"
        }
        return HorizontalLayout(labelSpan, spendSpan, countSpan, paceSpan).apply {
            isSpacing = true; isPadding = false
            style["width"] = "100%"; style["align-items"] = "center"
        }
    }

    private fun buildProfileCountRow(text: String): HorizontalLayout =
        HorizontalLayout(Span(text).apply {
            style["font-size"] = "var(--lumo-font-size-xs)"
            style["color"] = "var(--lumo-secondary-text-color)"
        }).apply { isPadding = false; isSpacing = false }

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
            style["font-size"] = "var(--lumo-font-size-m)"
            style["overflow"] = "hidden"
            style["text-overflow"] = "ellipsis"
            style["white-space"] = "nowrap"
        }
        return Div().apply {
            style["padding"] = "0.75rem"
            style["border-radius"] = "var(--lumo-border-radius-l)"
            style["background"] = "var(--lumo-base-color)"
            style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
            style["overflow"] = "hidden"
            val inner = VerticalLayout(iconDiv, labelSpan, valueSpan).apply {
                isPadding = false; isSpacing = false; style["gap"] = "0.3rem"
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

    private fun buildOriginBar(origin: String, count: Int, max: Int): HorizontalLayout {
        val pct = (count.toDouble() / max * 100).toInt()
        val bar = Div().apply {
            style["height"] = "8px"
            style["border-radius"] = "4px"
            style["background"] = "var(--lumo-primary-color)"
            style["width"] = "${pct}%"
            style["min-width"] = "8px"
            style["flex"] = "1"
        }
        val track = Div(bar).apply {
            style["flex"] = "1"
            style["background"] = "var(--lumo-contrast-10pct)"
            style["border-radius"] = "4px"
            style["height"] = "8px"
            style["display"] = "flex"; style["align-items"] = "stretch"
        }
        val nameLbl = Span(origin).apply {
            style["font-size"] = "var(--lumo-font-size-s)"; style["min-width"] = "80px"
        }
        val countLbl = Span("$count").apply {
            style["font-size"] = "var(--lumo-font-size-s)"
            style["color"] = "var(--lumo-secondary-text-color)"; style["min-width"] = "20px"
            style["text-align"] = "right"
        }
        return HorizontalLayout(nameLbl, track, countLbl).apply {
            isSpacing = true; isPadding = false
            style["align-items"] = "center"; style["width"] = "100%"
        }
    }

    private fun buildSpendList(container: VerticalLayout, data: Map<String, BigDecimal>) {
        container.removeAll()
        data.entries.sortedByDescending { it.value }.forEach { (name, amount) ->
            val row = HorizontalLayout(
                Span(name).apply {
                    style["flex"] = "1"; style["overflow"] = "hidden"
                    style["text-overflow"] = "ellipsis"; style["white-space"] = "nowrap"
                    style["font-size"] = "var(--lumo-font-size-s)"
                },
                Span(amount.formatPrice(getCurrency())).apply {
                    style["font-weight"] = "600"; style["font-size"] = "var(--lumo-font-size-s)"
                    style["color"] = "var(--lumo-primary-color)"; style["white-space"] = "nowrap"
                }
            ).apply {
                isSpacing = true; isPadding = false
                style["width"] = "100%"; style["align-items"] = "center"
            }
            container.add(row)
        }
    }

    private fun section(title: String, content: VerticalLayout): VerticalLayout =
        VerticalLayout().apply {
            isPadding = false; isSpacing = false; style["gap"] = "0.5rem"; width = "100%"
            add(H3(title).apply { style["margin"] = "0.5rem 0 0 0"; style["font-size"] = "var(--lumo-font-size-m)" })
            add(Div().apply {
                style["padding"] = "0.75rem"
                style["border-radius"] = "var(--lumo-border-radius-l)"
                style["background"] = "var(--lumo-base-color)"
                style["box-shadow"] = "0 1px 4px rgba(0,0,0,0.08)"
                add(content)
            })
        }
}

internal fun BigDecimal.formatPrice(currency: String = "€"): String = "${currency}${this.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()}"
