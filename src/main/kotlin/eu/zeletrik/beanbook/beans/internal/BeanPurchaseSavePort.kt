package eu.zeletrik.beanbook.beans.internal

import eu.zeletrik.beanbook.beans.BeanPurchase
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

interface BeanPurchaseSavePort {
    fun <S : BeanPurchase> save(entity: S): S
}

@Component
class JdbcBeanPurchaseSavePort(private val jdbcTemplate: JdbcTemplate) : BeanPurchaseSavePort {

    override fun <S : BeanPurchase> save(entity: S): S {
        jdbcTemplate.update(
            """
            INSERT OR REPLACE INTO bean_purchases (
                id, name, roaster, origin, price_per_unit, weight_grams,
                purchase_date, roast_date, roast_level, process,
                notes, grind_settings, image_data, rating,
                opened_date, finished_date
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            entity.id.toString(),
            entity.name,
            entity.roaster,
            entity.origin,
            entity.pricePerUnit.toPlainString(),
            entity.weightGrams,
            entity.purchaseDate.toString(),
            entity.roastDate.toString(),
            entity.roastLevel.name,
            entity.process.name,
            entity.notes,
            entity.grindSettings,
            entity.imageData,
            entity.rating,
            entity.openedDate?.toString(),
            entity.finishedDate?.toString(),
        )
        return entity
    }
}
