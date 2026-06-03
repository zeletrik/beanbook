package eu.zeletrik.beanbook.beans

import eu.zeletrik.beanbook.beans.internal.BeanPurchaseRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BeanPurchaseService(private val repository: BeanPurchaseRepository) {

    fun findAll(): List<BeanPurchase> = repository.findAll()

    fun save(purchase: BeanPurchase): BeanPurchase = repository.save(purchase)

    fun delete(id: UUID) = repository.delete(id)
}
