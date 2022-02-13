package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.Subject
import dev.domnikl.schema_registry_gitops.avroFromResources
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import org.junit.jupiter.api.assertThrows
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

        assertThrows<IllegalStateException> {
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
            logger.info("[GLOBAL]")
            logger.info("   ~ compatibility FULL -> FULL_TRANSITIVE")
            logger.info("")
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
        val schema = avroFromResources("schemas/key.avsc")
        val subject = Subject("foo", null, schema)

        every { client.subjects() } returns emptyList()
        every { client.create(subject) } returns 1

        val diff = Diffing.Result(added = listOf(subject))

        stateApplier.apply(diff)

        verifyOrder {
            client.create(subject)
            logger.info("[SUBJECT] foo")
            logger.info("   + registered (version 1)")
            logger.info("")
        }

        verify(exactly = 0) { client.updateCompatibility(subject) }
    }

    @Test
    fun `can create new subject with references`() {
        val schema = avroFromResources("schemas/key.avsc")
        val subject = Subject("foo", null, schema, listOf(SchemaReference("bar", "bar", 1)))

        every { client.subjects() } returns emptyList()
        every { client.create(subject) } returns 1

        val diff = Diffing.Result(added = listOf(subject))

        stateApplier.apply(diff)

        verifyOrder {
            client.create(subject)
            logger.info("[SUBJECT] foo")
            logger.info("   + registered (version 1)")
            logger.info("")
        }

        verify(exactly = 0) { client.updateCompatibility(subject) }
    }

    @Test
    fun `can register new subject and set compatibility`() {
        val schema = avroFromResources("schemas/key.avsc")
        val subject = Subject("foo", Compatibility.BACKWARD, schema)

        every { client.version(subject) } returns null
        every { client.create(subject) } returns 1
        every { client.updateCompatibility(subject) } returns Compatibility.BACKWARD

        val diff = Diffing.Result(added = listOf(subject))

        stateApplier.apply(diff)

        verifyOrder {
            client.create(subject)
            logger.info("[SUBJECT] foo")
            logger.info("   + registered (version 1)")
            client.updateCompatibility(subject)
            logger.info("   + compatibility BACKWARD")
            logger.info("")
        }
    }

    @Test
    fun `can evolve schema`() {
        val schema = avroFromResources("schemas/key.avsc")
        val subject = Subject("foo", null, schema)

        every { client.version(subject) } returns null
        every { client.evolve(subject) } returns 5

        val diff = Diffing.Result(
            modified = listOf(Diffing.Changes(subject, null, Diffing.Change(schema, schema)))
        )

        stateApplier.apply(diff)

        verifyOrder {
            logger.info("[SUBJECT] foo")
            client.evolve(subject)
            logger.info("   ~ evolved (version 5)")
            logger.info("   ~ schema   \"string\"")
            logger.info("")
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

            logger.info("[SUBJECT] foo")
            logger.info("   - deleted")
            logger.info("")
        }
    }

    @Test
    fun `can update compatibility and evolve schema`() {
        val schema = avroFromResources("schemas/key.avsc")
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
            logger.info("[SUBJECT] foo")
            client.updateCompatibility(subject)
            logger.info("   ~ compatibility FORWARD_TRANSITIVE -> FULL")
            client.evolve(subject)
            logger.info("   ~ evolved (version 2)")
            logger.info("   ~ schema   \"string\"")
            logger.info("")
        }
    }

    @Test
    fun `will not change subject compatibility if matches state`() {
        val schema = avroFromResources("schemas/key.avsc")
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
