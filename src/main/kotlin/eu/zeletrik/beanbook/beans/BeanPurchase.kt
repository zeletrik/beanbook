package eu.zeletrik.beanbook.beans

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class BeanPurchase(
    val id: UUID,
    val name: String,
    val roaster: String,
    val origin: String,
    val pricePerUnit: BigDecimal,
    val weightGrams: Int,
    val purchaseDate: LocalDate,
    val roastDate: LocalDate,
    val roastLevel: RoastLevel,
    val process: Process,
    val notes: String? = null,
    val grindSettings: String? = null,
    val imageData: ByteArray? = null,
    val rating: Int? = null,           // 1–5, null = not rated
    val openedDate: LocalDate? = null, // null = sealed
    val finishedDate: LocalDate? = null,
) {
    val bagState: BagState get() = when {
        finishedDate != null -> BagState.FINISHED
        openedDate != null   -> BagState.OPEN
        else                 -> BagState.SEALED
    }

}
