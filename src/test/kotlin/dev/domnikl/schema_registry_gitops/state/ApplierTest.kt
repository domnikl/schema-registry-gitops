package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import dev.domnikl.schema_registry_gitops.schemaFromResources
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.ConfigUpdateRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import org.slf4j.Logger

class ApplierTest {
    private val client = mockk<SchemaRegistryClient>()
    private val logger = mockk<Logger>(relaxed = true)
    private val stateApplier = Applier(client, logger)

    @Test
    fun `can apply default compatibility`() {
        val state = mockk<State>()

        every { state.compatibility } returns Compatibility.FULL_TRANSITIVE
        every { state.subjects } returns emptyList()

        val request = mockk<ConfigUpdateRequest>()
        every { request.compatibilityLevel } returns "FULL_TRANSITIVE"

        every { client.getCompatibility("") } returns "FULL"
        every { client.allSubjects } returns emptyList()
        every { client.updateCompatibility("", "FULL_TRANSITIVE") } returns "FULL_TRANSITIVE"

        stateApplier.apply(state)

        verifyOrder {
            client.updateCompatibility("", "FULL_TRANSITIVE")
            logger.info("Changed default compatibility level from FULL to FULL_TRANSITIVE")
        }
    }

    @Test
    fun `will not change default compatibility if matches state`() {
        val state = mockk<State>()

        every { state.compatibility } returns Compatibility.FULL_TRANSITIVE
        every { state.subjects } returns emptyList()

        val request = mockk<ConfigUpdateRequest>()
        every { request.compatibilityLevel } returns "FULL_TRANSITIVE"

        every { client.getCompatibility("") } returns "FULL_TRANSITIVE"
        every { client.allSubjects } returns emptyList()
        every { client.updateCompatibility("", "FULL_TRANSITIVE") } returns "FULL_TRANSITIVE"

        stateApplier.apply(state)

        verify(exactly = 0) { client.updateCompatibility("", "FULL_TRANSITIVE") }
        verify { logger.debug("Did not change compatibility level as it matched desired level FULL_TRANSITIVE") }
    }

    @Test
    fun `can register new subject`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(
            Subject("foo", null, schema)
        )

        every { client.allSubjects } returns emptyList()
        every { client.register("foo", schema) } returns 1

        stateApplier.apply(state)

        verify { client.register("foo", schema) }
        verify { logger.info("Registered new schema for 'foo' with version 1") }
        verify(exactly = 0) { client.updateCompatibility(any(), any()) }
    }

    @Test
    fun `can register new subject and set compatibility`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(
            Subject("foo", Compatibility.BACKWARD, schema)
        )

        every { client.allSubjects } returns emptyList()
        every { client.register("foo", schema) } returns 1
        every { client.updateCompatibility("foo", "BACKWARD") } returns "BACKWARD"

        stateApplier.apply(state)

        verifyOrder {
            client.register("foo", schema)
            logger.info("Registered new schema for 'foo' with version 1")
            client.updateCompatibility("foo", "BACKWARD")
            logger.info("Changed 'foo' compatibility to BACKWARD")
        }
    }

    @Test
    fun `can evolve schema`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(
            Subject("foo", null, schema)
        )

        every { client.allSubjects } returns listOf("foo")
        every { client.register("foo", schema) } returns 2

        stateApplier.apply(state)

        verifyOrder {
            client.register("foo", schema)
            logger.info("Evolved existing schema for 'foo' to version 2")
        }
        verify(exactly = 0) { client.updateCompatibility(any(), any()) }
    }

    @Test
    fun `can update compatibility and evolve schema`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(
            Subject("foo", Compatibility.FORWARD_TRANSITIVE, schema)
        )

        every { client.allSubjects } returns listOf("foo")
        every { client.updateCompatibility("foo", "FORWARD_TRANSITIVE") } returns "FORWARD_TRANSITIVE"
        every { client.register("foo", schema) } returns 2

        stateApplier.apply(state)

        verifyOrder {
            client.updateCompatibility("foo", "FORWARD_TRANSITIVE")
            logger.info("Changed 'foo' compatibility to FORWARD_TRANSITIVE")
            client.register("foo", schema)
            logger.info("Evolved existing schema for 'foo' to version 2")
        }
    }
}
