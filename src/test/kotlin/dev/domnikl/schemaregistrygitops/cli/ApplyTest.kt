package dev.domnikl.schemaregistrygitops.cli

import dev.domnikl.schemaregistrygitops.CLI
import dev.domnikl.schemaregistrygitops.Configuration
import dev.domnikl.schemaregistrygitops.State
import dev.domnikl.schemaregistrygitops.fromResources
import dev.domnikl.schemaregistrygitops.state.Applier
import dev.domnikl.schemaregistrygitops.state.Diffing
import dev.domnikl.schemaregistrygitops.state.Persistence
import dev.domnikl.schemaregistrygitops.state.Result
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
        every { applier.apply(any()) } returns Result.SUCCESS
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
        every { applier.apply(any()) } returns Result.ERROR
        every { logger.error(any()) } just runs

        val input = "only_compatibility.yml"
        val exitCode = commandLine.execute("apply", "--registry", "https://foo.bar", input)

        assertEquals(1, exitCode)

        verify { logger.error("java.lang.IllegalArgumentException: foobar") }
    }

    @Test
    fun `logs errors it encounters`() {
        every { persistence.load(any(), any()) } throws IllegalArgumentException("foobar")
        every { applier.apply(any()) } returns Result.ERROR
        every { logger.error(any()) } just runs

        val input = fromResources("only_compatibility.yml")
        val exitCode = commandLine.execute("apply", "--registry", "https://foo.bar", input.path)

        assertEquals(1, exitCode)

        verify { logger.error("java.lang.IllegalArgumentException: foobar") }
    }

    @Test
    fun `only normalize`() {
        val state = mockk<State>()
        val diffingResult = Diffing.Result(added = listOf(mockk()))

        every { configuration.baseUrl } returns "https://foo.bar"
        every { persistence.load(any(), any()) } returns state
        every { applier.apply(any()) } returns Result.SUCCESS
        every { logger.info(any()) } just runs
        every { diffing.diff(state, false) } returns diffingResult

        val input = fromResources("only_normalize.yml")
        val exitCode = commandLine.execute("apply", "--registry", "https://foo.bar", input.path)

        verify { logger.info("[SUCCESS] Applied state from ${input.path} to https://foo.bar") }

        assertEquals(0, exitCode)
    }
}
