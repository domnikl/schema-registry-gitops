package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.state.Dumper
import dev.domnikl.schema_registry_gitops.state.Persistence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertEquals
import org.junit.Test
import picocli.CommandLine
import java.io.File

class DumpTest {
    private val persistence = mockk<Persistence>()
    private val dumper = mockk<Dumper>()
    private val dump = Dump(persistence, dumper)
    private val commandLine = CommandLine(CLI()).addSubcommand(dump)

    @Test
    fun `can dump state to file`() {
        val tempFile = File.createTempFile(javaClass.simpleName, "can-dump-state-to-file")
        tempFile.deleteOnExit()

        val state = mockk<State>()

        every { dumper.dump() } returns state
        every { persistence.save(state, any()) } just runs

        val exitCode = commandLine.execute("dump", "--registry", "http://foo.bar", tempFile.path)

        assertEquals(0, exitCode)
    }

    @Test
    fun `can dump state to stdout`() {
        val state = mockk<State>()

        every { dumper.dump() } returns state
        every { persistence.save(state, System.out) } just runs

        val exitCode = commandLine.execute("dump", "--registry", "http://foo.bar")

        assertEquals(0, exitCode)
    }
}
