package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.Factory
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import dev.domnikl.schema_registry_gitops.state.Diffing
import dev.domnikl.schema_registry_gitops.state.Persistence
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
import java.io.File

class PlanTest {
    @Rule @JvmField val exit: ExpectedSystemExit = ExpectedSystemExit.none()

    private val diff = mockk<Diffing>()
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

        every { factory.diffing } returns diff
        every { factory.persistence } returns persistence
        every { persistence.load(any(), input) } returns state
        every { diff.diff(any()) } returns Diffing.Result(compatibility = Diffing.Change(Compatibility.NONE, Compatibility.BACKWARD))

        val exitCode = CLI.commandLine(factory, logger).execute("plan", "--registry", "https://foo.bar", input.path)

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

        every { factory.diffing } returns diff
        every { factory.persistence } returns persistence
        every { persistence.load(any(), input) } returns state
        every { diff.diff(any()) } returns Diffing.Result()

        val exitCode = CLI.commandLine(factory, logger).execute("plan", "--registry", "https://foo.bar", input.path)

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

        every { factory.diffing } returns diff
        every { factory.persistence } returns persistence
        every { persistence.load(any(), input) } returns state
        every { diff.diff(any()) } returns Diffing.Result(deleted = listOf("foobar"))

        val exitCode = CLI.commandLine(factory, logger).execute("plan", "--registry", "https://foo.bar", input.path)

        assertEquals(0, exitCode)

        verify {
            logger.info("[SUBJECT] foobar")
            logger.info("   ~ deleted")
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

        every { factory.diffing } returns diff
        every { factory.persistence } returns persistence
        every { persistence.load(any(), input) } returns state
        every { diff.diff(any()) } returns Diffing.Result(added = state.subjects)

        val exitCode = CLI.commandLine(factory, logger).execute("plan", "--registry", "https://foo.bar", input.path)

        assertEquals(0, exitCode)

        verify {
            logger.info("[SUBJECT] foo")
            logger.info("   ~ registered")
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

        val schemaBefore = mockk<ParsedSchema>()
        val schemaAfter = mockk<ParsedSchema>()

        every { schemaBefore.toString() } returns "<SCHEMA-BEFORE>"
        every { schemaAfter.toString() } returns "<SCHEMA-AFTER>"

        val input = fromResources("with_inline_schema.yml")

        every { factory.diffing } returns diff
        every { factory.persistence } returns persistence
        every { persistence.load(any(), input) } returns state
        every { diff.diff(any()) } returns Diffing.Result(
            modified = listOf(
                Diffing.Changes(
                    state.subjects.first(),
                    Diffing.Change(Compatibility.NONE, Compatibility.BACKWARD),
                    Diffing.Change(schemaBefore, schemaAfter)
                )
            )
        )

        val exitCode = CLI.commandLine(factory, logger).execute("plan", "--registry", "https://foo.bar", input.path)

        assertEquals(0, exitCode)

        verify {
            logger.info("[SUBJECT] foo")
            logger.info("   ~ compatibility NONE -> BACKWARD")
            logger.info("   ~ schema <SCHEMA-BEFORE> -> <SCHEMA-AFTER>")
            logger.info("")
            logger.info("[SUCCESS] All changes are compatible and can be applied.")
        }
        verify(exactly = 0) { logger.error(any()) }
    }

    @Test
    fun `can handle relative inputFile paths`() {
        val state = State(
            null,
            listOf(Subject("foo", null, mockk()))
        )

        val input = "with_inline_schema.yml"

        every { factory.diffing } returns diff
        every { factory.persistence } returns persistence
        every { persistence.load(any(), any()) } returns state
        every { diff.diff(any()) } returns Diffing.Result()

        val exitCode = CLI.commandLine(factory, logger).execute("plan", "--registry", "http://foo.bar", input)

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

        every { factory.diffing } returns diff
        every { factory.persistence } returns persistence
        every { persistence.load(any(), input) } returns state
        every { diff.diff(any()) } returns Diffing.Result(incompatible = state.subjects)

        val exitCode = CLI.commandLine(factory, logger).execute("plan", "--registry", "foo", input.path)

        assertEquals(1, exitCode)

        verifyOrder {
            logger.error("[ERROR] The following schemas are incompatible with an earlier version: 'foo', 'bar'")
        }
    }

    @Test
    fun `can report other errors`() {
        every { factory.diffing } returns diff
        every { diff.diff(any()) } throws IllegalArgumentException("foobar")

        val input = fromResources("with_inline_schema.yml")
        val exitCode = CLI.commandLine(factory, logger).execute("plan", "--registry", "foo", input.path)

        assertEquals(2, exitCode)

        verify { logger.error("java.lang.IllegalArgumentException: foobar") }
    }

    private fun fromResources(name: String) = File(javaClass.classLoader.getResource(name)!!.toURI())
}
