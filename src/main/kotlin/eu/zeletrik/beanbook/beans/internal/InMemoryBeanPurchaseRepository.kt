package eu.zeletrik.beanbook.beans.internal

import eu.zeletrik.beanbook.beans.BeanPurchase
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Repository
class InMemoryBeanPurchaseRepository : BeanPurchaseRepository {

    private val purchases: MutableList<BeanPurchase> = mutableListOf(
        BeanPurchase(
            id = UUID.randomUUID(),
            name = "Yirgacheffe Natural",
            roaster = "Square Mile",
            origin = "Ethiopia",
            pricePerUnit = BigDecimal("18.50"),
            weightGrams = 250,
            purchaseDate = LocalDate.of(2025, 3, 10),
            roastDate = LocalDate.of(2025, 3, 5),
            roastLevel = RoastLevel.LIGHT,
            process = Process.NATURAL,
            notes = "Blueberry and jasmine notes",
            rating = 5,
            openedDate = LocalDate.of(2025, 3, 12),
            finishedDate = LocalDate.of(2025, 4, 1),
        ),
        BeanPurchase(
            id = UUID.randomUUID(),
            name = "Huila Washed",
            roaster = "Onyx Coffee Lab",
            origin = "Colombia",
            pricePerUnit = BigDecimal("22.00"),
            weightGrams = 340,
            purchaseDate = LocalDate.of(2025, 4, 1),
            roastDate = LocalDate.of(2025, 3, 28),
            roastLevel = RoastLevel.MEDIUM,
            process = Process.WASHED,
            rating = 4,
            openedDate = LocalDate.of(2025, 4, 5),
        ),
        BeanPurchase(
            id = UUID.randomUUID(),
            name = "Sumatra Mandheling",
            roaster = "Intelligentsia",
            origin = "Indonesia",
            pricePerUnit = BigDecimal("16.00"),
            weightGrams = 250,
            purchaseDate = LocalDate.of(2025, 4, 15),
            roastDate = LocalDate.of(2025, 4, 10),
            roastLevel = RoastLevel.DARK,
            process = Process.WASHED,
            notes = "Earthy, low acidity",
            rating = 3,
            openedDate = LocalDate.of(2025, 4, 20),
            finishedDate = LocalDate.of(2025, 5, 10),
        ),
        BeanPurchase(
            id = UUID.randomUUID(),
            name = "Gesha Honey",
            roaster = "Onyx Coffee Lab",
            origin = "Panama",
            pricePerUnit = BigDecimal("42.00"),
            weightGrams = 100,
            purchaseDate = LocalDate.of(2025, 5, 2),
            roastDate = LocalDate.of(2025, 4, 29),
            roastLevel = RoastLevel.LIGHT,
            process = Process.HONEY,
        ),
        BeanPurchase(
            id = UUID.randomUUID(),
            name = "Sidama Washed",
            roaster = "Square Mile",
            origin = "Ethiopia",
            pricePerUnit = BigDecimal("19.00"),
            weightGrams = 250,
            purchaseDate = LocalDate.of(2025, 5, 20),
            roastDate = LocalDate.of(2025, 5, 16),
            roastLevel = RoastLevel.MEDIUM,
            process = Process.WASHED,
            rating = 4,
            openedDate = LocalDate.of(2025, 5, 22),
        ),
    )

    override fun findAll(): List<BeanPurchase> = purchases.toList()

    override fun save(purchase: BeanPurchase): BeanPurchase {
        val index = purchases.indexOfFirst { it.id == purchase.id }
        if (index >= 0) {
            purchases[index] = purchase
        } else {
            purchases.add(purchase)
        }
        return purchase
    }

    override fun delete(id: UUID) {
        purchases.removeIf { it.id == id }
    }
}
