package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.state.Dumper
import dev.domnikl.schema_registry_gitops.state.Persistence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertEquals
import org.junit.Test

class DumpTest {
    private val dumper = mockk<Dumper>()
    private val statePersistence = mockk<Persistence>()
    private val factory = mockk<Factory>()

    @Test
    fun `can dump state to file`() {
        val state = mockk<State>()

        every { factory.createStateDumper(any()) } returns dumper
        every { dumper.dump() } returns state
        every { factory.createStatePersistence() } returns statePersistence
        every { statePersistence.save(state, any()) } just runs

        val exitCode = CLI.commandLine(factory).execute("dump", "--registry", "http://foo.bar", "foo.yml")

        assertEquals(0, exitCode)
    }
}
