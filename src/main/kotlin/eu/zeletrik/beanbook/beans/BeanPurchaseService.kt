package eu.zeletrik.beanbook.beans

import eu.zeletrik.beanbook.beans.internal.BeanPurchaseRepository
import eu.zeletrik.beanbook.beans.internal.BeanPurchaseSavePort
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BeanPurchaseService(
    private val repository: BeanPurchaseRepository,
    private val savePort: BeanPurchaseSavePort,
) {

    fun findAll(): List<BeanPurchase> = repository.findAll()

    fun save(purchase: BeanPurchase): BeanPurchase = savePort.save<BeanPurchase>(purchase)

    fun delete(id: UUID) = repository.deleteById(id)
}
