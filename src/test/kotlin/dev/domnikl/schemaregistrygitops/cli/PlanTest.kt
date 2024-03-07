package dev.domnikl.schemaregistrygitops.cli

import dev.domnikl.schemaregistrygitops.CLI
import dev.domnikl.schemaregistrygitops.Compatibility
import dev.domnikl.schemaregistrygitops.Configuration
import dev.domnikl.schemaregistrygitops.State
import dev.domnikl.schemaregistrygitops.Subject
import dev.domnikl.schemaregistrygitops.state.Diffing
import dev.domnikl.schemaregistrygitops.state.Persistence
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import org.slf4j.Logger
import picocli.CommandLine
import java.io.File

class PlanTest {
    @Rule
    @JvmField
    val exit: ExpectedSystemExit = ExpectedSystemExit.none()

    private val configuration = mockk<Configuration>()
    private val diffing = mockk<Diffing>()
    private val persistence = mockk<Persistence>()
    private val logger = mockk<Logger>(relaxed = true)
    private val plan = Plan(configuration, persistence, diffing, logger)
    private val commandLine = CommandLine(CLI()).addSubcommand(plan)

    @Test
    fun `can validate YAML state file`() {
        val state = State(
            null,
            listOf(Subject("foo", null, mockk()))
        )

        val input = fromResources("with_inline_schema.yml")

        every { persistence.load(any(), input) } returns state
        every { diffing.diff(any()) } returns Diffing.Result(compatibility = Diffing.Change(Compatibility.NONE, Compatibility.BACKWARD))

        val exitCode = commandLine.execute("plan", "--registry", "https://foo.bar", *input.map { it.path }.toTypedArray())

        assertEquals(0, exitCode)

        verify {
            logger.info("[GLOBAL]")
            logger.info("   ~ compatibility NONE -> BACKWARD")
            logger.info("")
            logger.info("[SUCCESS] All changes are compatible and can be applied.")
        }
        verify(exactly = 0) { logger.error(any()) }
    }

    @Test
    fun `will log success when no changes were made`() {
        val state = State(
            null,
            listOf(Subject("foo", null, mockk()))
        )

        val input = fromResources("with_inline_schema.yml")

        every { persistence.load(any(), input) } returns state
        every { diffing.diff(any()) } returns Diffing.Result()

        val exitCode = commandLine.execute("plan", "--registry", "https://foo.bar", *input.map { it.path }.toTypedArray())

        assertEquals(0, exitCode)

        verify {
            logger.info("[SUCCESS] There are no necessary changes; the actual state matches the desired state.")
        }
        verify(exactly = 0) { logger.error(any()) }
    }

    @Test
    fun `will log deletes`() {
        val state = State(
            null,
            listOf(Subject("foo", null, mockk()))
        )

        val input = fromResources("with_inline_schema.yml")

        every { persistence.load(any(), input) } returns state
        every { diffing.diff(any(), true) } returns Diffing.Result(deleted = listOf("foobar"))

        val exitCode =
            commandLine.execute("plan", "--enable-deletes", "--registry", "https://foo.bar", *input.map { it.path }.toTypedArray())

        assertEquals(0, exitCode)

        verify {
            logger.info("The following changes would be applied:")
            logger.info("")
            logger.info("[SUBJECT] foobar")
            logger.info("   - delete")
            logger.info("")
            logger.info("[SUCCESS] All changes are compatible and can be applied.")
        }
        verify(exactly = 0) { logger.error(any()) }
    }

    @Test
    fun `will log adds`() {
        val state = State(
            null,
            listOf(Subject("foo", null, mockk()))
        )

        val input = fromResources("with_inline_schema.yml")

        every { persistence.load(any(), input) } returns state
        every { diffing.diff(any()) } returns Diffing.Result(added = state.subjects)

        val exitCode = commandLine.execute("plan", "--registry", "https://foo.bar", *input.map { it.path }.toTypedArray())

        assertEquals(0, exitCode)

        verify {
            logger.info("The following changes would be applied:")
            logger.info("")
            logger.info("[SUBJECT] foo")
            logger.info("   + register")
            logger.info("")
            logger.info("[SUCCESS] All changes are compatible and can be applied.")
        }
        verify(exactly = 0) { logger.error(any()) }
    }

    @Test
    fun `will log changes`() {
        val state = State(
            null,
            listOf(Subject("foo", null, mockk()))
        )

        val schemaBefore = mockk<ParsedSchema>(relaxed = true)
        val schemaAfter = mockk<ParsedSchema>(relaxed = true)

        val input = fromResources("with_inline_schema.yml")

        every { persistence.load(any(), input) } returns state
        every { diffing.diff(any()) } returns Diffing.Result(
            modified = listOf(
                Diffing.Changes(
                    state.subjects.first(),
                    Diffing.Change(Compatibility.NONE, Compatibility.BACKWARD),
                    Diffing.Change(schemaBefore, schemaAfter)
                )
            )
        )

        val exitCode = commandLine.execute("plan", "--registry", "https://foo.bar", *input.map { it.path }.toTypedArray())

        verify {
            logger.info("The following changes would be applied:")
            logger.info("")
            logger.info("[SUBJECT] foo")
            logger.info("   ~ compatibility NONE -> BACKWARD")
            logger.info("   ~ schema   ")
            logger.info("")
            logger.info("[SUCCESS] All changes are compatible and can be applied.")
        }

        verify(exactly = 0) { logger.error(any()) }

        assertEquals(0, exitCode)
    }

    @Test
    fun `can handle relative inputFile paths`() {
        val state = State(
            null,
            listOf(Subject("foo", null, mockk()))
        )

        val input = "with_inline_schema.yml"

        every { persistence.load(any(), any()) } returns state
        every { diffing.diff(any()) } returns Diffing.Result()

        val exitCode = commandLine.execute("plan", "--registry", "https://foo.bar", input)

        assertEquals(0, exitCode)
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

        every { persistence.load(any(), input) } returns state
        every { diffing.diff(any()) } returns Diffing.Result(
            incompatible = listOf(
                Diffing.CompatibilityTestResult(
                    state.subjects[0],
                    listOf("my message")
                )
            )
        )

        val exitCode = commandLine.execute("plan", "--registry", "foo", *input.map { it.path }.toTypedArray())

        assertEquals(1, exitCode)

        verifyOrder {
            logger.error("[ERROR] The following schema is incompatible with an earlier version: 'foo': 'my message'")
        }
    }

    @Test
    fun `can report other errors`() {
        val state = State(
            null,
            listOf(
                Subject("foo", null, mockk()),
                Subject("bar", null, mockk())
            )
        )

        every { persistence.load(any(), any()) } returns state
        every { diffing.diff(any()) } throws IllegalArgumentException("foobar")

        val input = fromResources("with_inline_schema.yml")
        val exitCode = commandLine.execute("plan", "--registry", "foo", *input.map { it.path }.toTypedArray())

        assertEquals(2, exitCode)

        verify { logger.error("java.lang.IllegalArgumentException: foobar") }
    }

    private fun fromResources(name: String) = listOf(File(javaClass.classLoader.getResource(name)!!.toURI()))
}
