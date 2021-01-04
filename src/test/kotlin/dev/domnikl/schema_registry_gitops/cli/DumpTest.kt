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
import java.io.File

class DumpTest {
    private val dumper = mockk<Dumper>()
    private val statePersistence = mockk<Persistence>()
    private val factory = mockk<Factory>(relaxed = true)

    @Test
    fun `can dump state to file`() {
        val tempFile = File.createTempFile(javaClass.simpleName, "can-dump-state-to-file")
        tempFile.deleteOnExit()

        val state = mockk<State>()

        every { factory.dumper } returns dumper
        every { dumper.dump() } returns state
        every { factory.persistence } returns statePersistence
        every { statePersistence.save(state, any()) } just runs

        val exitCode = CLI.commandLine(factory).execute("dump", "--registry", "http://foo.bar", tempFile.path)

        assertEquals(0, exitCode)
    }

    @Test
    fun `can dump state to stdout`() {
        val state = mockk<State>()

        every { factory.dumper } returns dumper
        every { dumper.dump() } returns state
        every { factory.persistence } returns statePersistence
        every { statePersistence.save(state, System.out) } just runs

        val exitCode = CLI.commandLine(factory).execute("dump", "--registry", "http://foo.bar")

        assertEquals(0, exitCode)
    }
}
