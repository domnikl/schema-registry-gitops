package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import dev.domnikl.schema_registry_gitops.state.Validator
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import org.slf4j.Logger
import java.io.File

class ValidateTest {
    @Rule @JvmField val exit: ExpectedSystemExit = ExpectedSystemExit.none()

    private val validator = mockk<Validator>()
    private val factory = mockk<Factory>()
    private val logger = mockk<Logger>()

    @Test
    fun `can validate YAML state file`() {
        every { factory.createValidator(any()) } returns validator
        every { validator.validate(any()) } returns emptyList()
        every { logger.debug(any()) } just runs
        every { logger.error(any()) } just runs

        val input = fromResources("with_inline_schema.yml")
        val exitCode = CLI.commandLine(factory, logger).execute("validate", "--registry", "http://foo.bar", input.path)

        assertEquals(0, exitCode)

        verify { logger.debug("VALIDATION PASSED: all schemas are ready to be evolved") }
        verify(exactly = 0) { logger.error(any()) }
    }

    @Test
    fun `can report validation fails`() {
        every { factory.createValidator(any()) } returns validator
        every { validator.validate(any()) } returns listOf("foo", "bar")
        every { logger.error(any()) } just runs

        val input = fromResources("with_inline_schema.yml")
        val exitCode = CLI.commandLine(factory, logger).execute("validate", input.path)

        assertEquals(1, exitCode)

        verify {
            logger.error("VALIDATION FAILED: The following schemas are incompatible with an earlier version: 'foo', 'bar'")
        }
    }

    @Test
    fun `can report other errors`() {
        every { factory.createValidator(any()) } returns validator
        every { validator.validate(any()) } throws IllegalArgumentException("foobar")
        every { logger.error(any()) } just runs

        val input = fromResources("with_inline_schema.yml")
        val exitCode = CLI.commandLine(factory, logger).execute("validate", input.path)

        assertEquals(2, exitCode)

        verify { logger.error("java.lang.IllegalArgumentException: foobar") }
    }

    private fun fromResources(name: String) = File(javaClass.classLoader.getResource(name)!!.toURI())
}
