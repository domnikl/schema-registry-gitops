package dev.domnikl.schema_registry_gitops.command

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.StateApplier
import dev.domnikl.schema_registry_gitops.StatePersistence
import dev.domnikl.schema_registry_gitops.fromResources
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyTest {
    private val applier = mockk<StateApplier>()
    private val statePersistence = mockk<StatePersistence>()
    private val factory = mockk<Factory>()

    @Test
    fun `can dump state to file`() {
        val state = mockk<State>()

        every { factory.createStateApplier(any()) } returns applier
        every { factory.createStatePersistence() } returns statePersistence
        every { statePersistence.load(any(), any()) } returns state
        every { applier.apply(state) } just runs

        val input = fromResources("only_compatibility.yml")
        val exitCode = CLI.commandLine(factory).execute("apply", "--registry", "http://foo.bar", input.path)

        assertEquals(0, exitCode)
    }
}
