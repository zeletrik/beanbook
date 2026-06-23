package eu.zeletrik.beanbook

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

/** Verifies that the application's Spring Modulith module boundaries are valid and free of illegal cross-module dependencies. */
class ModuleBoundaryTest {

    @Test
    fun `module boundaries are valid`() {
        ApplicationModules.of(BeanbookApplication::class.java).verify()
    }
}
