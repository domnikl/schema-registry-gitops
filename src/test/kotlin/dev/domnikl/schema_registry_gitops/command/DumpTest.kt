package dev.domnikl.schema_registry_gitops.command

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.StateDumper
import dev.domnikl.schema_registry_gitops.StatePersistence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertEquals
import org.junit.Test

class DumpTest {
    private val dumper = mockk<StateDumper>()
    private val statePersistence = mockk<StatePersistence>()
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
