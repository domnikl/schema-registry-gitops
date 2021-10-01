package dev.domnikl.schema_registry_gitops

import dev.domnikl.schema_registry_gitops.state.Persistence
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class FactoryTest {
    @Test
    fun `can create persistence after injecting config`() {
        val config = mockk<Configuration>(relaxed = true)

        every { config.toMap() } returns emptyMap()

        val factory = Factory().also { it.inject(config) }
        val persistence = factory.persistence

        assert(persistence is Persistence)
    }
}
