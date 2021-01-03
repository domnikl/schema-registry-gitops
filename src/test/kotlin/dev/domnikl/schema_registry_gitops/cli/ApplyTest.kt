package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.fromResources
import dev.domnikl.schema_registry_gitops.state.Applier
import dev.domnikl.schema_registry_gitops.state.Persistence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyTest {
    private val applier = mockk<Applier>()
    private val statePersistence = mockk<Persistence>()
    private val factory = mockk<Factory>()

    @Test
    fun `can apply state to schema registry`() {
        val state = mockk<State>()

        every { factory.createApplier(any()) } returns applier
        every { factory.createPersistence() } returns statePersistence
        every { statePersistence.load(any(), any()) } returns state
        every { applier.apply(state) } just runs

        val input = fromResources("only_compatibility.yml")
        val exitCode = CLI.commandLine(factory).execute("apply", "--registry", "http://foo.bar", input.path)

        assertEquals(0, exitCode)
    }
}
