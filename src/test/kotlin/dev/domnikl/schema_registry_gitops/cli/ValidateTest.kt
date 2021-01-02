package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import dev.domnikl.schema_registry_gitops.state.Validator
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class ValidateTest {
    @Rule @JvmField val exit: ExpectedSystemExit = ExpectedSystemExit.none()

    private val err = ByteArrayOutputStream()
    private val out = ByteArrayOutputStream()
    private val oldErr: PrintStream = System.err
    private val oldOut: PrintStream = System.out

    private val validator = mockk<Validator>()
    private val factory = mockk<Factory>()

    @Before
    fun setupStreams() {
        out.reset()
        err.reset()
        System.setOut(PrintStream(out))
        System.setErr(PrintStream(err))
    }

    @After
    fun restoreStreams() {
        System.setOut(oldOut)
        System.setErr(oldErr)
    }

    @Test
    fun `can validate YAML state file`() {
        every { factory.createStateValidator(any()) } returns validator
        every { validator.validate(any()) } returns emptyList()

        val input = fromResources("with_inline_schema.yml")
        val exitCode = CLI.commandLine(factory).execute("validate", "--registry", "http://foo.bar", input.path)

        assertEquals("", out.toString())
        assertEquals("", err.toString())
        assertEquals(0, exitCode)
    }

    @Test
    fun `can report validation fails`() {
        every { factory.createStateValidator(any()) } returns validator
        every { validator.validate(any()) } returns listOf("foo", "bar")

        val input = fromResources("with_inline_schema.yml")
        val exitCode = CLI.commandLine(factory).execute("validate", input.path)

        val expectedOutput =
            """
            The following schemas are incompatible with an earlier version:
            
              - foo
              - bar
            
            VALIDATION FAILED
            
            """.trimIndent()

        assertEquals(expectedOutput, out.toString())
        assertEquals("", err.toString())
        assertEquals(1, exitCode)
    }

    private fun fromResources(name: String) = File(javaClass.classLoader.getResource(name)!!.toURI())
}
