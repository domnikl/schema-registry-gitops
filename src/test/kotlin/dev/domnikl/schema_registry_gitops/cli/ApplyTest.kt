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
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.Logger

class ApplyTest {
    private val applier = mockk<Applier>()
    private val statePersistence = mockk<Persistence>()
    private val factory = mockk<Factory>(relaxed = true)
    private val logger = mockk<Logger>()

    @Test
    fun `can apply state to schema registry`() {
        val state = mockk<State>()

        every { factory.applier } returns applier
        every { factory.persistence } returns statePersistence
        every { statePersistence.load(any(), any()) } returns state
        every { applier.apply(state) } just runs
        every { logger.info(any()) } just runs

        val input = fromResources("only_compatibility.yml")
        val exitCode = CLI.commandLine(factory, logger).execute("apply", "--registry", "http://foo.bar", input.path)

        assertEquals(0, exitCode)

        verify { logger.info("Successfully applied state from ${input.path} to http://foo.bar") }
    }

    @Test
    fun `logs errors it encounters`() {
        val state = mockk<State>()

        every { factory.applier } returns applier
        every { factory.persistence } returns statePersistence
        every { statePersistence.load(any(), any()) } throws IllegalArgumentException("foobar")
        every { applier.apply(state) } just runs
        every { logger.error(any()) } just runs

        val input = fromResources("only_compatibility.yml")
        val exitCode = CLI.commandLine(factory, logger).execute("apply", "--registry", "http://foo.bar", input.path)

        assertEquals(1, exitCode)

        verify { logger.error("java.lang.IllegalArgumentException: foobar") }
    }
}
