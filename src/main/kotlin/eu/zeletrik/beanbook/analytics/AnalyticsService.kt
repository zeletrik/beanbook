package eu.zeletrik.beanbook.analytics

import eu.zeletrik.beanbook.beans.BeanPurchase
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class AnalyticsService {

    fun totalCost(purchases: List<BeanPurchase>): BigDecimal =
        purchases.fold(BigDecimal.ZERO) { acc, p -> acc + p.pricePerUnit }

    fun totalSpendByBean(purchases: List<BeanPurchase>): Map<String, BigDecimal> =
        purchases.groupBy { it.name }
            .mapValues { (_, ps) -> ps.fold(BigDecimal.ZERO) { acc, p -> acc + p.pricePerUnit } }

    fun totalSpendByRoaster(purchases: List<BeanPurchase>): Map<String, BigDecimal> =
        purchases.groupBy { it.roaster }
            .mapValues { (_, ps) -> ps.fold(BigDecimal.ZERO) { acc, p -> acc + p.pricePerUnit } }

    fun averageCost(purchases: List<BeanPurchase>): BigDecimal {
        if (purchases.isEmpty()) return BigDecimal.ZERO
        return totalCost(purchases).divide(BigDecimal(purchases.size), 2, RoundingMode.HALF_UP)
    }

    fun originBreakdown(purchases: List<BeanPurchase>): Map<String, Int> =
        purchases.groupingBy { it.origin }.eachCount()

    fun mostCommonOrigin(purchases: List<BeanPurchase>): String? {
        if (purchases.isEmpty()) return null
        val maxCount = originBreakdown(purchases).values.max()
        return originBreakdown(purchases)
            .filter { (_, count) -> count == maxCount }
            .keys
            .minOrNull()
    }

    fun mostExpensiveBean(purchases: List<BeanPurchase>): BeanPurchase? {
        if (purchases.isEmpty()) return null
        val maxPrice = purchases.maxOf { it.pricePerUnit }
        return purchases
            .filter { it.pricePerUnit == maxPrice }
            .minByOrNull { it.purchaseDate }
    }

    fun mostExpensiveRoaster(purchases: List<BeanPurchase>): String? {
        if (purchases.isEmpty()) return null
        val averageByRoaster = purchases.groupBy { it.roaster }
            .mapValues { (_, ps) ->
                ps.fold(BigDecimal.ZERO) { acc, p -> acc + p.pricePerUnit }
                    .divide(BigDecimal(ps.size), 10, RoundingMode.HALF_UP)
            }
        val maxAverage = averageByRoaster.values.max()
        return averageByRoaster
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
}
