package eu.zeletrik.beanbook.beans

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("bean_purchases")
data class BeanPurchase(
    @Id
    val id: UUID,
    val name: String,
    val roaster: String,
    val origin: String,
    @Column("price_per_unit")
    val pricePerUnit: BigDecimal,
    @Column("weight_grams")
    val weightGrams: Int,
    @Column("purchase_date")
    val purchaseDate: LocalDate,
    @Column("roast_date")
    val roastDate: LocalDate,
    @Column("roast_level")
    val roastLevel: RoastLevel,
    val process: Process,
    val notes: String? = null,
    @Column("grind_settings")
    val grindSettings: String? = null,
    @Column("image_data")
    val imageData: ByteArray? = null,
    val rating: Int? = null,
    @Column("opened_date")
    val openedDate: LocalDate? = null,
    @Column("finished_date")
    val finishedDate: LocalDate? = null,
    @Column("roast_profile")
    val roastProfile: RoastProfile,
    @Column("used_as")
    val usedAs: RoastProfile? = null,
    @Column("tags")
    val tags: List<String> = emptyList(),
) {
    @get:Transient
    val bagState: BagState
        get() = when {
            finishedDate != null -> BagState.FINISHED
            openedDate != null   -> BagState.OPEN
            else                 -> BagState.SEALED
        }
}
