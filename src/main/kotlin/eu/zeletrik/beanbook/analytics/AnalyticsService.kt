package eu.zeletrik.beanbook.analytics

import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.RoastProfile
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/** Total spend in a calendar month, for the spend-over-time chart. */
data class MonthlySpend(val month: YearMonth, val total: BigDecimal)

/** Computes spending, pace, and breakdown statistics over a list of [BeanPurchase] records for the analytics dashboard. */
@Service
class AnalyticsService {

    fun totalCost(purchases: List<BeanPurchase>): BigDecimal =
        purchases.fold(BigDecimal.ZERO) { acc, p -> acc + p.price }

    /**
     * Spend per purchase-month over the most recent [months]-month window ending at the latest
     * purchase, with empty months zero-filled so the timeline is continuous (not gap-collapsed).
     */
    fun spendByMonth(purchases: List<BeanPurchase>, months: Int = 6): List<MonthlySpend> {
        if (purchases.isEmpty()) return emptyList()
        val totals = purchases
            .groupBy { YearMonth.from(it.purchaseDate) }
            .mapValues { (_, ps) -> ps.fold(BigDecimal.ZERO) { acc, p -> acc + p.price } }
        val end = totals.keys.max()
        val start = end.minusMonths((months - 1).toLong())
        return generateSequence(start) { if (it < end) it.plusMonths(1) else null }
            .map { MonthlySpend(it, totals[it] ?: BigDecimal.ZERO) }
            .toList()
    }

    fun totalSpendByBean(purchases: List<BeanPurchase>): Map<String, BigDecimal> =
        purchases.groupBy { it.name }
            .mapValues { (_, ps) -> ps.fold(BigDecimal.ZERO) { acc, p -> acc + p.price } }

    fun totalSpendByRoaster(purchases: List<BeanPurchase>): Map<String, BigDecimal> =
        purchases.groupBy { it.roaster }
            .mapValues { (_, ps) -> ps.fold(BigDecimal.ZERO) { acc, p -> acc + p.price } }

    fun averageCost(purchases: List<BeanPurchase>): BigDecimal {
        if (purchases.isEmpty()) return BigDecimal.ZERO
        return totalCost(purchases).divide(BigDecimal(purchases.size), 2, RoundingMode.HALF_UP)
    }

    fun originBreakdown(purchases: List<BeanPurchase>): Map<String, Int> =
        purchases.groupingBy { it.origin }.eachCount()

    fun mostCommonOrigin(purchases: List<BeanPurchase>): String? {
        if (purchases.isEmpty()) return null
        val breakdown = originBreakdown(purchases)
        val maxCount = breakdown.values.max()
        return breakdown
            .filter { (_, count) -> count == maxCount }
            .keys
            .minOrNull()
    }

    /**
     * The priciest bean normalised by weight (price per gram), so a small premium bag isn't beaten by
     * a large cheap one. [BeanPurchase.weightGrams] is always > 0; ties break by earliest purchase.
     */
    fun mostExpensiveBean(purchases: List<BeanPurchase>): BeanPurchase? {
        if (purchases.isEmpty()) return null
        val maxPerGram = purchases.maxOf { it.pricePerGram() }
        return purchases
            .filter { it.pricePerGram() == maxPerGram }
            .minByOrNull { it.purchaseDate }
    }

    private fun BeanPurchase.pricePerGram(): BigDecimal =
        price.divide(BigDecimal(weightGrams), 10, RoundingMode.HALF_UP)

    /**
     * The roaster whose beans are priciest on average, normalised by weight (mean price per gram)
     * so a roaster of small premium bags isn't beaten by one selling large cheap ones. Ties break
     * by alphabetical roaster name.
     */
    fun mostExpensiveRoaster(purchases: List<BeanPurchase>): String? {
        if (purchases.isEmpty()) return null
        val avgPerGramByRoaster = purchases.groupBy { it.roaster }
            .mapValues { (_, ps) ->
                ps.fold(BigDecimal.ZERO) { acc, p -> acc + p.pricePerGram() }
                    .divide(BigDecimal(ps.size), 10, RoundingMode.HALF_UP)
            }
        val maxAverage = avgPerGramByRoaster.values.max()
        return avgPerGramByRoaster
            .filter { (_, avg) -> avg == maxAverage }
            .keys
            .minOrNull()
    }

    fun projectedMonthlyCost(purchases: List<BeanPurchase>): BigDecimal? {
        if (purchases.isEmpty()) return null
        val avgPace = averagePaceDays(purchases) ?: return null
        val avgCost = averageCost(purchases)
        val result = BigDecimal("30").divide(avgPace, 10, RoundingMode.HALF_UP)
            .multiply(avgCost)
            .setScale(2, RoundingMode.HALF_UP)
        return result.max(BigDecimal("0.01"))
    }

    fun averagePaceDays(purchases: List<BeanPurchase>): BigDecimal? {
        val durations = purchases
            .filter { it.openedDate != null && it.finishedDate != null && !it.finishedDate.isBefore(it.openedDate) }
            .map { maxOf(1L, java.time.temporal.ChronoUnit.DAYS.between(it.openedDate, it.finishedDate)).toBigDecimal() }
        if (durations.isEmpty()) return null
        return durations.fold(BigDecimal.ZERO) { acc, d -> acc + d }
            .divide(BigDecimal(durations.size), 1, RoundingMode.HALF_UP)
    }

    fun spendByBrewMethod(purchases: List<BeanPurchase>): Map<BrewMethod, BigDecimal> {
        val base = BrewMethod.entries.associateWith { BigDecimal.ZERO }.toMutableMap()
        purchases.forEach { p ->
            val method = p.effectiveBrewMethod()
            base[method] = (base[method] ?: BigDecimal.ZERO) + p.price
        }
        return base
    }

    fun countByBrewMethod(purchases: List<BeanPurchase>): Map<BrewMethod, Int> {
        val base = BrewMethod.entries.associateWith { 0 }.toMutableMap()
        purchases.forEach { p ->
            val method = p.effectiveBrewMethod()
            base[method] = (base[method] ?: 0) + 1
        }
        return base
    }

    fun paceByBrewMethod(purchases: List<BeanPurchase>): Map<BrewMethod, BigDecimal?> {
        return mapOf(
            BrewMethod.ESPRESSO to averagePaceDays(purchases.filter { it.effectiveBrewMethod() == BrewMethod.ESPRESSO }),
            BrewMethod.FILTER   to averagePaceDays(purchases.filter { it.effectiveBrewMethod() == BrewMethod.FILTER }),
        )
    }

    fun countByRoastProfile(purchases: List<BeanPurchase>): Map<RoastProfile, Int> {
        val base = RoastProfile.entries.associateWith { 0 }.toMutableMap()
        purchases.forEach { p -> base[p.roastProfile] = (base[p.roastProfile] ?: 0) + 1 }
        return base
    }
}
