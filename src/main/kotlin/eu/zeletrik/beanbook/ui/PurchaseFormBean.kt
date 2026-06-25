package eu.zeletrik.beanbook.ui

import eu.zeletrik.beanbook.beans.BrewTarget
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel
import eu.zeletrik.beanbook.beans.RoastProfile
import java.math.BigDecimal
import java.time.LocalDate

class PurchaseFormBean {
    var name: String = ""
    var roaster: String = ""
    var origin: String = ""
    var region: String = ""
    var price: BigDecimal? = null
    var weightGrams: Int? = null
    var purchaseDate: LocalDate? = null
    var roastDate: LocalDate? = null
    var roastLevel: RoastLevel? = null
    var process: Process? = null
    var notes: String = ""
    var grindSettings: String = ""
    var imageData: ByteArray? = null
    var rating: Int? = null
    var openedDate: LocalDate? = null
    var finishedDate: LocalDate? = null
    var roastProfile: RoastProfile? = RoastProfile.OMNI
    var usedAs: BrewTarget? = null
    var tags: Set<String> = emptySet()
    var url: String = ""
}
