package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import dev.domnikl.schema_registry_gitops.state.Persistence
import dev.domnikl.schema_registry_gitops.state.Validator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import org.slf4j.Logger
import java.io.File

class ValidateTest {
    @Rule @JvmField val exit: ExpectedSystemExit = ExpectedSystemExit.none()

    private val validator = mockk<Validator>()
    private val persistence = mockk<Persistence>()
    private val factory = mockk<Factory>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    @Test
    fun `can validate YAML state file`() {
        val state = State(
            null,
            listOf(Subject("foo", null, mockk()))
        )

        val input = fromResources("with_inline_schema.yml")

        every { factory.validator } returns validator
        every { factory.persistence } returns persistence
        every { persistence.load(any(), input) } returns state
        every { validator.validate(any()) } returns emptyList()

        val exitCode = CLI.commandLine(factory, logger).execute("validate", "--registry", "http://foo.bar", input.path)

        assertEquals(0, exitCode)

        verify {
            logger.info("Subject 'foo': ok")
            logger.info("VALIDATION PASSED: all schemas are ready to be evolved")
        }
        verify(exactly = 0) { logger.error(any()) }
    }

    @Test
    fun `can report validation fails`() {
        val state = State(
            null,
            listOf(
                Subject("foo", null, mockk()),
                Subject("bar", null, mockk())
            )
        )

        val input = fromResources("with_inline_schema.yml")

        every { factory.validator } returns validator
        every { factory.persistence } returns persistence
        every { persistence.load(any(), input) } returns state
        every { validator.validate(any()) } returns listOf("foo", "bar")

        val exitCode = CLI.commandLine(factory, logger).execute("validate", input.path)

        assertEquals(1, exitCode)

        verifyOrder {
            logger.error("Subject 'foo': FAIL")
            logger.error("Subject 'bar': FAIL")
            logger.error("VALIDATION FAILED: The following schemas are incompatible with an earlier version: 'foo', 'bar'")
        }
    }

    @Test
    fun `can report other errors`() {
        every { factory.validator } returns validator
        every { validator.validate(any()) } throws IllegalArgumentException("foobar")

        val input = fromResources("with_inline_schema.yml")
        val exitCode = CLI.commandLine(factory, logger).execute("validate", input.path)

        assertEquals(2, exitCode)

        verify { logger.error("java.lang.IllegalArgumentException: foobar") }
    }

    private fun fromResources(name: String) = File(javaClass.classLoader.getResource(name)!!.toURI())
}
