package eu.zeletrik.beanbook

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModuleBoundaryTest {

    @Test
    fun `module boundaries are valid`() {
        ApplicationModules.of(BeanbookApplication::class.java).verify()
    }
}
