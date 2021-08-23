package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.Subject
import dev.domnikl.schema_registry_gitops.schemaFromResources
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertThrows
import org.junit.Test
import org.slf4j.Logger

class ApplierTest {
    private val client = mockk<SchemaRegistryClient>()
    private val logger = mockk<Logger>(relaxed = true)
    private val stateApplier = Applier(client, logger)

    @Test
    fun `throws exception when trying to apply with incompatibilities`() {
        val schema = mockk<ParsedSchema>()
        val subject = Subject("foo", null, schema)

        val diff = Diffing.Result(incompatible = listOf(subject))

        assertThrows(IllegalStateException::class.java) {
            stateApplier.apply(diff)
        }
    }

    @Test
    fun `can apply global compatibility`() {
        every { client.updateGlobalCompatibility(Compatibility.FULL_TRANSITIVE) } returns Compatibility.FULL_TRANSITIVE

        val diff = Diffing.Result(compatibility = Diffing.Change(Compatibility.FULL, Compatibility.FULL_TRANSITIVE))

        stateApplier.apply(diff)

        verifyOrder {
            client.updateGlobalCompatibility(Compatibility.FULL_TRANSITIVE)
            logger.info("Changed global compatibility level from FULL to FULL_TRANSITIVE")
        }
    }

    @Test
    fun `will not change global compatibility if matches state`() {
        val diff = Diffing.Result()

        stateApplier.apply(diff)

        verify(exactly = 0) { client.updateGlobalCompatibility(Compatibility.FULL_TRANSITIVE) }
    }

    @Test
    fun `can create new subject`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val subject = Subject("foo", null, schema)

        every { client.subjects() } returns emptyList()
        every { client.create(subject) } returns 1

        val diff = Diffing.Result(added = listOf(subject))

        stateApplier.apply(diff)

        verify { client.create(subject) }
        verify { logger.info("Created subject 'foo' and registered new schema with version 1") }
        verify(exactly = 0) { client.updateCompatibility(subject) }
    }

    @Test
    fun `can register new subject and set compatibility`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val subject = Subject("foo", Compatibility.BACKWARD, schema)

        every { client.version(subject) } returns null
        every { client.create(subject) } returns 1
        every { client.updateCompatibility(subject) } returns Compatibility.BACKWARD

        val diff = Diffing.Result(added = listOf(subject))

        stateApplier.apply(diff)

        verifyOrder {
            client.create(subject)
            logger.info("Created subject 'foo' and registered new schema with version 1")
            client.updateCompatibility(subject)
            logger.info("Changed 'foo' compatibility from NONE to BACKWARD")
        }
    }

    @Test
    fun `can evolve schema`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val subject = Subject("foo", null, schema)

        every { client.version(subject) } returns null
        every { client.evolve(subject) } returns 5

        val diff = Diffing.Result(
            modified = listOf(Diffing.Changes(subject, null, Diffing.Change(schema, schema)))
        )

        stateApplier.apply(diff)

        verifyOrder {
            client.evolve(subject)
            logger.info("Evolved existing schema for subject 'foo' to version 5")
        }
        verify(exactly = 0) { client.updateCompatibility(subject) }
    }

    @Test
    fun `can delete subject`() {
        val diff = Diffing.Result(deleted = listOf("foo"))

        every { client.delete("foo") } just runs

        stateApplier.apply(diff)

        verifyOrder {
            client.delete("foo")
            logger.info("Deleted subject 'foo'")
        }
    }

    @Test
    fun `will not evolve schema if version already exists`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val subject = Subject("foo", null, schema)

        every { client.version(subject) } returns 2

        val diff = Diffing.Result(
            modified = listOf(Diffing.Changes(subject, null, Diffing.Change(schema, schema)))
        )

        stateApplier.apply(diff)

        verifyOrder {
            client.version(subject)
            logger.debug("Did not evolve schema, version already exists as 2")
        }
        verify(exactly = 0) { client.updateCompatibility(subject) }
    }

    @Test
    fun `can update compatibility and evolve schema`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val subject = Subject("foo", Compatibility.FORWARD_TRANSITIVE, schema)

        every { client.version(subject) } returns null
        every { client.updateCompatibility(subject) } returns Compatibility.FULL
        every { client.evolve(subject) } returns 2

        val diff = Diffing.Result(
            modified = listOf(
                Diffing.Changes(
                    subject,
                    Diffing.Change(Compatibility.FORWARD_TRANSITIVE, Compatibility.FULL),
                    Diffing.Change(schema, schema)
                )
            )
        )

        stateApplier.apply(diff)

        verifyOrder {
            client.updateCompatibility(subject)
            logger.info("Changed 'foo' compatibility from FORWARD_TRANSITIVE to FULL")
            client.version(subject)
            client.evolve(subject)
            logger.info("Evolved existing schema for subject 'foo' to version 2")
        }
    }

    @Test
    fun `will not change subject compatibility if matches state`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val subject = Subject("foo", Compatibility.FULL, schema)

        every { client.version(subject) } returns 1

        val diff = Diffing.Result(
            modified = listOf(
                Diffing.Changes(
                    subject,
                    null,
                    null
                )
            )
        )

        stateApplier.apply(diff)

        verify(exactly = 0) {
            client.version(subject)
            client.updateCompatibility(subject)
        }
    }
}
