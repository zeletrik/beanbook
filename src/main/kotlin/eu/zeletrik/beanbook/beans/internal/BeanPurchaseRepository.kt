package eu.zeletrik.beanbook.beans.internal

import eu.zeletrik.beanbook.beans.BeanPurchase
import org.springframework.data.repository.ListCrudRepository
import java.util.UUID

interface BeanPurchaseRepository : ListCrudRepository<BeanPurchase, UUID>
