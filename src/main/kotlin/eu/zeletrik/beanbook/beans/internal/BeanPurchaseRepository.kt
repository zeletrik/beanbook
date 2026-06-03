package eu.zeletrik.beanbook.beans.internal

import eu.zeletrik.beanbook.beans.BeanPurchase
import java.util.UUID

interface BeanPurchaseRepository {
    fun findAll(): List<BeanPurchase>
    fun save(purchase: BeanPurchase): BeanPurchase
    fun delete(id: UUID)
}
