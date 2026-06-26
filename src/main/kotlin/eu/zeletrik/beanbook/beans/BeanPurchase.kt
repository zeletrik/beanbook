package eu.zeletrik.beanbook.beans

import com.fasterxml.jackson.annotation.JsonAlias
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** A coffee bean purchase persisted as a [Persistable] entity; insert-vs-update is decided explicitly via [markNew]. */
@Table("bean_purchases")
data class BeanPurchase(
    @Id
    @get:JvmName("idValue")
    val id: UUID,
    val name: String,
    val roaster: String,
    val origin: String,
    /**
     * Whole-bag price. Physical column keeps its original name; `@JsonAlias` keeps pre-rename
     * backup exports (which used "pricePerUnit") importable.
     */
    @Column("price_per_unit")
    @JsonAlias("pricePerUnit")
    val price: BigDecimal,
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
    val usedAs: BrewTarget? = null,
    @Column("tags")
    val tags: Set<String> = emptySet(),
    /** Optional link to the bean's product page or the roaster's profile. */
    @Column("url")
    val url: String? = null,
    /** Optional second-level origin (region / sub-origin), e.g. "Huila" for a Colombia bean. */
    @Column("region")
    val region: String? = null,
) : Persistable<UUID> {

    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(price > BigDecimal.ZERO) { "price must be positive" }
        require(weightGrams > 0) { "weightGrams must be positive" }
        rating?.let { require(it in 1..5) { "rating must be between 1 and 5" } }
        require(roastProfile == RoastProfile.OMNI || usedAs == null) {
            "usedAs is only meaningful for OMNI beans"
        }
    }

    /**
     * The id getter is renamed (`@get:JvmName`) so this explicit override can satisfy
     * [Persistable.getId] without a JVM signature clash. Kotlin code still uses `.id`.
     */
    override fun getId(): UUID = id

    /**
     * Identity is the id alone: a data class equals() over all fields would use referential
     * equality for the imageData ByteArray (so equal beans compare unequal), and value equality
     * is wrong for a persisted entity anyway.
     */
    override fun equals(other: Any?): Boolean =
        this === other || (other is BeanPurchase && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    /**
     * Spring Data JDBC treats an entity with a populated non-null @Id as already-persisted (UPDATE).
     * Bean ids are app-assigned UUIDs, so insert-vs-update is decided explicitly: BeanPurchaseService
     * sets this from existsById before each save. @Transient → never persisted; not a constructor
     * property → excluded from equals/hashCode/copy.
     */
    @Transient
    private var newEntity: Boolean = true

    @Transient
    override fun isNew(): Boolean = newEntity

    /** Set by BeanPurchaseService before save() to choose INSERT (true) vs UPDATE (false). */
    fun markNew(isNew: Boolean): BeanPurchase = apply { newEntity = isNew }

    @get:Transient
    val bagState: BagState
        get() = when {
            finishedDate != null -> BagState.FINISHED
            openedDate != null   -> BagState.OPEN
            else                 -> BagState.SEALED
        }
}
