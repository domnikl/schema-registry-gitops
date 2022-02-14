package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Configuration
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.fromResources
import dev.domnikl.schema_registry_gitops.state.Applier
import dev.domnikl.schema_registry_gitops.state.Diffing
import dev.domnikl.schema_registry_gitops.state.Persistence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.Logger
import picocli.CommandLine

class ApplyTest {
    private val configuration = mockk<Configuration>()
    private val persistence = mockk<Persistence>()
    private val diffing = mockk<Diffing>()
    private val applier = mockk<Applier>()
    private val logger = mockk<Logger>(relaxed = true)
    private val apply = Apply(configuration, persistence, diffing, applier, logger)
    private val commandLine = CommandLine(CLI()).addSubcommand(apply)

    @Test
    fun `can apply state to schema registry`() {
        val state = mockk<State>()
        val diffingResult = Diffing.Result(added = listOf(mockk()))

        every { configuration.baseUrl } returns "https://foo.bar"
        every { persistence.load(any(), any()) } returns state
        every { applier.apply(any()) } just runs
        every { logger.info(any()) } just runs
        every { diffing.diff(state, false) } returns diffingResult

        val input = fromResources("only_compatibility.yml")
        val exitCode = commandLine.execute("apply", "--registry", "https://foo.bar", input.path)

        verify { logger.info("[SUCCESS] Applied state from ${input.path} to https://foo.bar") }

        assertEquals(0, exitCode)
    }

    @Test
    fun `can handle relative inputFile paths`() {
        every { persistence.load(any(), any()) } throws IllegalArgumentException("foobar")
        every { applier.apply(any()) } just runs
        every { logger.error(any()) } just runs

        val input = "only_compatibility.yml"
        val exitCode = commandLine.execute("apply", "--registry", "https://foo.bar", input)

        assertEquals(1, exitCode)

        verify { logger.error("java.lang.IllegalArgumentException: foobar") }
    }

    @Test
    fun `logs errors it encounters`() {
        every { persistence.load(any(), any()) } throws IllegalArgumentException("foobar")
        every { applier.apply(any()) } just runs
        every { logger.error(any()) } just runs

        val input = fromResources("only_compatibility.yml")
        val exitCode = commandLine.execute("apply", "--registry", "https://foo.bar", input.path)

        assertEquals(1, exitCode)

        verify { logger.error("java.lang.IllegalArgumentException: foobar") }
    }
}
