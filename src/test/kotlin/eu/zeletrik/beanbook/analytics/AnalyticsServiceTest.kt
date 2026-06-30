package eu.zeletrik.beanbook.analytics

import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.BrewTarget
import eu.zeletrik.beanbook.beans.RoastProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class AnalyticsServiceTest {

    private val service = AnalyticsService()

    private fun purchase(
        name: String = "Bean",
        roaster: String = "Roaster",
        origin: String = "Ethiopia",
        price: String = "10.00",
        purchaseDate: LocalDate = LocalDate.of(2025, 1, 1),
        roastProfile: RoastProfile = RoastProfile.FILTER,
        usedAs: BrewTarget? = null,
    ) = BeanPurchase(
        id = UUID.randomUUID(),
        name = name,
        roaster = roaster,
        origin = origin,
        price = BigDecimal(price),
        weightGrams = 250,
        purchaseDate = purchaseDate,
        roastDate = purchaseDate.minusDays(5),
        roastLevel = RoastLevel.MEDIUM,
        process = Process.WASHED,
        imageData = null,
        roastProfile = roastProfile,
        usedAs = usedAs,
    )

    private val samplePurchases = listOf(
        purchase(name = "Yirgacheffe", roaster = "Square Mile", origin = "Ethiopia", price = "18.50"),
        purchase(name = "Huila", roaster = "Onyx", origin = "Colombia", price = "22.00"),
        purchase(name = "Sumatra", roaster = "Intelligentsia", origin = "Indonesia", price = "16.00"),
        purchase(name = "Gesha", roaster = "Onyx", origin = "Panama", price = "42.00"),
        purchase(name = "Sidama", roaster = "Square Mile", origin = "Ethiopia", price = "19.00"),
    )

    // AC-18: total cost
    @Test
    fun `totalCost returns sum of all price`() {
        val result = service.totalCost(samplePurchases)
        assertEquals(BigDecimal("117.50"), result)
    }

    // AC-19: total cost empty list
    @Test
    fun `totalCost returns zero for empty list`() {
        assertEquals(BigDecimal.ZERO, service.totalCost(emptyList()))
    }

    // #9: spend-over-time aggregation — by purchase month, zero-filled, within the window.
    @Test
    fun `spendByMonth sums by purchase month and zero-fills gaps within the window`() {
        val beans = listOf(
            purchase(price = "10.00", purchaseDate = LocalDate.of(2025, 1, 15)),
            purchase(price = "5.00", purchaseDate = LocalDate.of(2025, 1, 20)),
            purchase(price = "20.00", purchaseDate = LocalDate.of(2025, 3, 3)),
        )
        val result = service.spendByMonth(beans, months = 6)

        assertEquals(6, result.size, "window must be exactly 6 months")
        assertEquals(java.time.YearMonth.of(2025, 3), result.last().month, "window ends at the latest purchase month")
        assertEquals(BigDecimal("15.00"), result.first { it.month == java.time.YearMonth.of(2025, 1) }.total)
        assertEquals(BigDecimal.ZERO, result.first { it.month == java.time.YearMonth.of(2025, 2) }.total, "gap month is zero-filled")
        assertEquals(BigDecimal("20.00"), result.last().total)
    }

    @Test
    fun `spendByMonth returns empty for no purchases`() {
        assertTrue(service.spendByMonth(emptyList()).isEmpty())
    }

    // §D4.6: `price` is the whole-bag price — totals must NOT scale with weight.
    @Test
    fun `totalCost is the sum of bag prices and independent of weight`() {
        val light = purchase(price = "12.00").copy(weightGrams = 200)
        val heavy = purchase(price = "12.00").copy(weightGrams = 1000)
        assertEquals(BigDecimal("24.00"), service.totalCost(listOf(light, heavy)))
    }

    // AC-20: total spend per bean
    @Test
    fun `totalSpendByBean groups purchases by name`() {
        val result = service.totalSpendByBean(samplePurchases)
        assertEquals(BigDecimal("18.50"), result["Yirgacheffe"])
        assertEquals(BigDecimal("42.00"), result["Gesha"])
    }

    // AC-21: total spend per roaster
    @Test
    fun `totalSpendByRoaster sums purchases by roaster`() {
        val result = service.totalSpendByRoaster(samplePurchases)
        assertEquals(BigDecimal("37.50"), result["Square Mile"])
        assertEquals(BigDecimal("64.00"), result["Onyx"])
    }

    // AC-22: average cost
    @Test
    fun `averageCost returns mean price`() {
        val result = service.averageCost(samplePurchases)
        assertEquals(BigDecimal("23.50"), result)
    }

    // AC-23: average cost empty list
    @Test
    fun `averageCost returns zero for empty list`() {
        assertEquals(BigDecimal.ZERO, service.averageCost(emptyList()))
    }

    // AC-24: origin breakdown
    @Test
    fun `originBreakdown counts purchases per origin`() {
        val result = service.originBreakdown(samplePurchases)
        assertEquals(2, result["Ethiopia"])
        assertEquals(1, result["Colombia"])
        assertEquals(1, result["Indonesia"])
        assertEquals(1, result["Panama"])
    }

    // AC-25: most common origin — happy path
    @Test
    fun `mostCommonOrigin returns origin with highest count`() {
        assertEquals("Ethiopia", service.mostCommonOrigin(samplePurchases))
    }

    // AC-25: most common origin — tie-breaking: lexicographically first
    @Test
    fun `mostCommonOrigin resolves tie by lexicographic order`() {
        val tied = listOf(
            purchase(origin = "Colombia"),
            purchase(origin = "Ethiopia"),
        )
        assertEquals("Colombia", service.mostCommonOrigin(tied))
    }

    // AC-25: empty list
    @Test
    fun `mostCommonOrigin returns null for empty list`() {
        assertNull(service.mostCommonOrigin(emptyList()))
    }

    // AC-26: most expensive bean — happy path
    @Test
    fun `mostExpensiveBean returns entry with highest price`() {
        val result = service.mostExpensiveBean(samplePurchases)
        assertEquals("Gesha", result?.name)
        assertEquals(BigDecimal("42.00"), result?.price)
    }

    // AC-26: most expensive bean — tie-breaking: earliest purchaseDate
    @Test
    fun `mostExpensiveBean resolves tie by earliest purchaseDate`() {
        val earlier = purchase(name = "Alpha", price = "30.00", purchaseDate = LocalDate.of(2025, 1, 1))
        val later = purchase(name = "Beta", price = "30.00", purchaseDate = LocalDate.of(2025, 6, 1))
        val result = service.mostExpensiveBean(listOf(later, earlier))
        assertEquals("Alpha", result?.name)
    }

    // AC-26: empty list
    @Test
    fun `mostExpensiveBean returns null for empty list`() {
        assertNull(service.mostExpensiveBean(emptyList()))
    }

    // Priciest is normalised by weight: a small premium bag beats a larger cheaper one even though
    // its whole-bag price is lower (€30/100g = €0.30/g  vs  €40/1000g = €0.04/g).
    @Test
    fun `mostExpensiveBean normalises by weight, not whole-bag price`() {
        val smallPremium = purchase(name = "Gesha 100g", price = "30.00").copy(weightGrams = 100)
        val bigCheap = purchase(name = "House 1kg", price = "40.00").copy(weightGrams = 1000)
        val result = service.mostExpensiveBean(listOf(bigCheap, smallPremium))
        assertEquals("Gesha 100g", result?.name)
    }

    // AC-27: most expensive roaster — happy path
    @Test
    fun `mostExpensiveRoaster returns roaster with highest count-weighted average price`() {
        // Same-weight bags (250 g), so ranking tracks price. Global mean = 117.50/5 = 23.50; smoothing C = 5.
        // Onyx:           (64.00 + 5·23.50) / (2+5) = 25.929  ← highest
        // Intelligentsia: (16.00 + 5·23.50) / (1+5) = 22.250
        // Square Mile:    (37.50 + 5·23.50) / (2+5) = 22.143
        assertEquals("Onyx", service.mostExpensiveRoaster(samplePurchases))
    }

    // #26: count-weighting must stop one dear bag from topping a roaster that is consistently expensive.
    @Test
    fun `mostExpensiveRoaster shrinks a single dear bag below a consistently expensive roaster`() {
        val budget = (1..4).map { purchase(roaster = "Budget", price = "5.00").copy(weightGrams = 100) }     // 0.05/g
        val frequent = (1..5).map { purchase(roaster = "Frequent", price = "10.00").copy(weightGrams = 100) } // 0.10/g
        val oneOff = listOf(purchase(roaster = "One-Off", price = "11.00").copy(weightGrams = 100))           // 0.11/g
        // Plain mean would crown "One-Off" (0.11 > 0.10). Global mean €/g = 0.081, smoothing C = 5:
        //   Frequent: (0.50 + 5·0.081) / (5+5) = 0.0905   ← highest
        //   One-Off:  (0.11 + 5·0.081) / (1+5) = 0.08583
        assertEquals("Frequent", service.mostExpensiveRoaster(budget + frequent + oneOff))
    }

    // AC-27: most expensive roaster — tie-breaking: lexicographically first
    @Test
    fun `mostExpensiveRoaster resolves tie by lexicographic order`() {
        val tied = listOf(
            purchase(roaster = "Zebra Roasters", price = "20.00"),
            purchase(roaster = "Alpha Roasters", price = "20.00"),
        )
        assertEquals("Alpha Roasters", service.mostExpensiveRoaster(tied))
    }

    // AC-27: empty list
    @Test
    fun `mostExpensiveRoaster returns null for empty list`() {
        assertNull(service.mostExpensiveRoaster(emptyList()))
    }

    // Top Roaster is normalised by weight (mean price per gram), so a roaster of small premium bags
    // beats one selling larger cheaper bags despite a lower per-bag price.
    @Test
    fun `mostExpensiveRoaster normalises by weight, not whole-bag price`() {
        val premium = purchase(roaster = "Tiny Premium", price = "30.00").copy(weightGrams = 100) // 0.30/g
        val bulk = purchase(roaster = "Bulk Cheap", price = "40.00").copy(weightGrams = 1000)      // 0.04/g
        assertEquals("Tiny Premium", service.mostExpensiveRoaster(listOf(bulk, premium)))
    }

    // ── Consumption Pace (AC-3 through AC-11) ────────────────────

    private fun finishedPurchase(
        openedDaysAgo: Long,
        finishedDaysAgo: Long,
        price: String = "15.00",
    ): BeanPurchase {
        val today = LocalDate.of(2025, 6, 1)
        return purchase(price = price).copy(
            openedDate = today.minusDays(openedDaysAgo),
            finishedDate = today.minusDays(finishedDaysAgo),
        )
    }

    // AC-3: happy path average pace
    @Test
    fun `averagePaceDays returns mean duration of finished bags`() {
        val bags = listOf(
            finishedPurchase(openedDaysAgo = 30, finishedDaysAgo = 10), // 20 days
            finishedPurchase(openedDaysAgo = 25, finishedDaysAgo = 5),  // 20 days
        )
        assertEquals(BigDecimal("20.0"), service.averagePaceDays(bags))
    }

    // AC-5 / AC-7: null when no finished bags / empty list
    @Test
    fun `averagePaceDays returns null for empty list`() {
        assertNull(service.averagePaceDays(emptyList()))
    }

    @Test
    fun `averagePaceDays returns null when no bags are finished`() {
        val unfinished = listOf(purchase()) // no openedDate or finishedDate
        assertNull(service.averagePaceDays(unfinished))
    }

    // AC-9: erroneous bag (finishedDate < openedDate) excluded
    @Test
    fun `averagePaceDays excludes bag where finishedDate is before openedDate`() {
        val today = LocalDate.of(2025, 6, 1)
        val valid = purchase().copy(
            openedDate = today.minusDays(20),
            finishedDate = today.minusDays(0),
        ) // 20 days
        val erroneous = purchase().copy(
            openedDate = today.minusDays(5),
            finishedDate = today.minusDays(10), // finishedDate BEFORE openedDate
        )
        assertEquals(BigDecimal("20.0"), service.averagePaceDays(listOf(valid, erroneous)))
    }

    // AC-10: same-day bag counts as 1 day
    @Test
    fun `averagePaceDays counts same-day bag as 1 day`() {
        val today = LocalDate.of(2025, 6, 1)
        val sameDay = purchase().copy(openedDate = today, finishedDate = today)
        assertEquals(BigDecimal("1.0"), service.averagePaceDays(listOf(sameDay)))
    }

    // AC-4: happy path projected monthly cost
    @Test
    fun `projectedMonthlyCost returns correct monthly projection`() {
        // avgPace = 20 days, avgCost = 15.00 → 30/20 * 15.00 = 22.50
        val bags = listOf(
            finishedPurchase(openedDaysAgo = 30, finishedDaysAgo = 10, price = "15.00"),
            finishedPurchase(openedDaysAgo = 25, finishedDaysAgo = 5, price = "15.00"),
        )
        assertEquals(BigDecimal("22.50"), service.projectedMonthlyCost(bags))
    }

    // AC-6 / AC-8: null when no finished bags / empty list
    @Test
    fun `projectedMonthlyCost returns null for empty list`() {
        assertNull(service.projectedMonthlyCost(emptyList()))
    }

    @Test
    fun `projectedMonthlyCost returns null when no bags are finished`() {
        assertNull(service.projectedMonthlyCost(listOf(purchase())))
    }

    // AC-11: minimum €0.01 enforced
    @Test
    fun `projectedMonthlyCost clamps result to minimum of 0_01`() {
        // avgPace = 3000 days, avgCost = 0.001 → 30/3000 * 0.001 ≈ 0.000010 → clamped to 0.01
        val today = LocalDate.of(2025, 6, 1)
        val verySlowBag = purchase(price = "0.001").copy(
            openedDate = today.minusDays(3000),
            finishedDate = today,
        )
        val result = service.projectedMonthlyCost(listOf(verySlowBag))
        assertEquals(BigDecimal("0.01"), result)
    }

    // ── effectiveBrewMethod classification (AC-19 through AC-23) ──

    @Test
    fun `ESPRESSO profile classifies as ESPRESSO`() {
        assertEquals(BrewMethod.ESPRESSO, purchase(roastProfile = RoastProfile.ESPRESSO).effectiveBrewMethod())
    }

    @Test
    fun `FILTER profile classifies as FILTER`() {
        assertEquals(BrewMethod.FILTER, purchase(roastProfile = RoastProfile.FILTER).effectiveBrewMethod())
    }

    @Test
    fun `OMNI with usedAs ESPRESSO classifies as ESPRESSO`() {
        assertEquals(BrewMethod.ESPRESSO, purchase(roastProfile = RoastProfile.OMNI, usedAs = BrewTarget.ESPRESSO).effectiveBrewMethod())
    }

    @Test
    fun `OMNI with usedAs FILTER classifies as FILTER`() {
        assertEquals(BrewMethod.FILTER, purchase(roastProfile = RoastProfile.OMNI, usedAs = BrewTarget.FILTER).effectiveBrewMethod())
    }

    @Test
    fun `OMNI with null usedAs classifies as UNCLASSIFIED`() {
        assertEquals(BrewMethod.UNCLASSIFIED, purchase(roastProfile = RoastProfile.OMNI, usedAs = null).effectiveBrewMethod())
    }

    // ── spendByBrewMethod (AC-24) ──

    @Test
    fun `spendByBrewMethod groups spend by effective brew method`() {
        val beans = listOf(
            purchase(price = "10.00", roastProfile = RoastProfile.ESPRESSO),
            purchase(price = "20.00", roastProfile = RoastProfile.FILTER),
            purchase(price = "15.00", roastProfile = RoastProfile.OMNI, usedAs = BrewTarget.ESPRESSO),
            purchase(price = "5.00",  roastProfile = RoastProfile.OMNI, usedAs = null),
        )
        val result = service.spendByBrewMethod(beans)
        assertEquals(BigDecimal("25.00"), result[BrewMethod.ESPRESSO])
        assertEquals(BigDecimal("20.00"), result[BrewMethod.FILTER])
        assertEquals(BigDecimal("5.00"),  result[BrewMethod.UNCLASSIFIED])
    }

    @Test
    fun `spendByBrewMethod returns zero for absent groups`() {
        val result = service.spendByBrewMethod(emptyList())
        assertEquals(BigDecimal.ZERO, result[BrewMethod.ESPRESSO])
        assertEquals(BigDecimal.ZERO, result[BrewMethod.FILTER])
        assertEquals(BigDecimal.ZERO, result[BrewMethod.UNCLASSIFIED])
    }

    // ── countByBrewMethod (AC-25) ──

    @Test
    fun `countByBrewMethod counts bags per effective brew method`() {
        val beans = listOf(
            purchase(roastProfile = RoastProfile.ESPRESSO),
            purchase(roastProfile = RoastProfile.ESPRESSO),
            purchase(roastProfile = RoastProfile.FILTER),
            purchase(roastProfile = RoastProfile.OMNI, usedAs = null),
        )
        val result = service.countByBrewMethod(beans)
        assertEquals(2, result[BrewMethod.ESPRESSO])
        assertEquals(1, result[BrewMethod.FILTER])
        assertEquals(1, result[BrewMethod.UNCLASSIFIED])
    }

    // ── paceByBrewMethod (AC-26, AC-27, AC-28) ──

    @Test
    fun `paceByBrewMethod returns pace for ESPRESSO and FILTER groups`() {
        val today = LocalDate.of(2025, 6, 1)
        val beans = listOf(
            purchase(roastProfile = RoastProfile.ESPRESSO).copy(
                openedDate = today.minusDays(10), finishedDate = today),   // 10 days
            purchase(roastProfile = RoastProfile.FILTER).copy(
                openedDate = today.minusDays(20), finishedDate = today),   // 20 days
        )
        val result = service.paceByBrewMethod(beans)
        assertEquals(BigDecimal("10.0"), result[BrewMethod.ESPRESSO])
        assertEquals(BigDecimal("20.0"), result[BrewMethod.FILTER])
    }

    @Test
    fun `paceByBrewMethod returns null when no qualifying bags for a method`() {
        val result = service.paceByBrewMethod(emptyList())
        assertNull(result[BrewMethod.ESPRESSO])
        assertNull(result[BrewMethod.FILTER])
    }

    @Test
    fun `paceByBrewMethod does not include UNCLASSIFIED key`() {
        val result = service.paceByBrewMethod(listOf(purchase(roastProfile = RoastProfile.OMNI)))
        assertNull(result[BrewMethod.UNCLASSIFIED], "UNCLASSIFIED must not appear in paceByBrewMethod result")
        assertEquals(setOf(BrewMethod.ESPRESSO, BrewMethod.FILTER), result.keys)
    }

    // ── countByRoastProfile (AC-29) ──

    @Test
    fun `countByRoastProfile counts bags by raw roastProfile`() {
        val beans = listOf(
            purchase(roastProfile = RoastProfile.ESPRESSO),
            purchase(roastProfile = RoastProfile.ESPRESSO),
            purchase(roastProfile = RoastProfile.FILTER),
            purchase(roastProfile = RoastProfile.OMNI),
            purchase(roastProfile = RoastProfile.OMNI),
            purchase(roastProfile = RoastProfile.OMNI),
        )
        val result = service.countByRoastProfile(beans)
        assertEquals(2, result[RoastProfile.ESPRESSO])
        assertEquals(1, result[RoastProfile.FILTER])
        assertEquals(3, result[RoastProfile.OMNI])
    }
}
