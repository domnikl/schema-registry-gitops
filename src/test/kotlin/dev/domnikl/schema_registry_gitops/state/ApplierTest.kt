package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import dev.domnikl.schema_registry_gitops.schemaFromResources
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
    fun `can apply global compatibility`() {
        val state = mockk<State>()

        every { state.compatibility } returns Compatibility.FULL_TRANSITIVE
        every { state.subjects } returns emptyList()

        every { client.globalCompatibility() } returns Compatibility.FULL
        every { client.subjects() } returns emptyList()
        every { client.updateGlobalCompatibility(Compatibility.FULL_TRANSITIVE) } returns Compatibility.FULL_TRANSITIVE

        stateApplier.apply(state)

        verifyOrder {
            client.updateGlobalCompatibility(Compatibility.FULL_TRANSITIVE)
            logger.info("Changed global compatibility level from FULL to FULL_TRANSITIVE")
        }
    }

    @Test
    fun `will not change global compatibility if matches state`() {
        val state = mockk<State>()

        every { state.compatibility } returns Compatibility.FULL_TRANSITIVE
        every { state.subjects } returns emptyList()

        every { client.globalCompatibility() } returns Compatibility.FULL_TRANSITIVE
        every { client.subjects() } returns emptyList()

        stateApplier.apply(state)

        verify(exactly = 0) { client.updateGlobalCompatibility(Compatibility.FULL_TRANSITIVE) }
        verify { logger.debug("Did not change compatibility level as it matched desired level FULL_TRANSITIVE") }
    }

    @Test
    fun `can create new subject`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()
        val subject = Subject("foo", null, schema)

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(subject)

        every { client.subjects() } returns emptyList()
        every { client.create(subject) } returns 1

        stateApplier.apply(state)

        verify { client.create(subject) }
        verify { logger.info("Created subject 'foo' and registered new schema with version 1") }
        verify(exactly = 0) { client.updateCompatibility(subject) }
    }

    @Test
    fun `can register new subject and set compatibility`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()
        val subject = Subject("foo", Compatibility.BACKWARD, schema)

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(subject)

        every { client.subjects() } returns emptyList()
        every { client.version(subject) } returns null
        every { client.create(subject) } returns 1
        every { client.compatibility("foo") } returns Compatibility.NONE
        every { client.updateCompatibility(subject) } returns Compatibility.BACKWARD

        stateApplier.apply(state)

        verifyOrder {
            client.create(subject)
            logger.info("Created subject 'foo' and registered new schema with version 1")
            client.updateCompatibility(subject)
            logger.info("Changed 'foo' compatibility to BACKWARD")
        }
    }

    @Test
    fun `can evolve schema`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()
        val subject = Subject("foo", null, schema)

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(subject)

        every { client.subjects() } returns listOf("foo")
        every { client.version(subject) } returns null
        every { client.evolve(subject) } returns 5

        stateApplier.apply(state)

        verifyOrder {
            client.evolve(subject)
            logger.info("Evolved existing schema for subject 'foo' to version 5")
        }
        verify(exactly = 0) { client.updateCompatibility(subject) }
    }

    @Test
    fun `will not evolve schema if version already exists`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()
        val subject = Subject("foo", null, schema)

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(subject)

        every { client.subjects() } returns listOf("foo")
        every { client.version(subject) } returns 2

        stateApplier.apply(state)

        verifyOrder {
            client.version(subject)
            logger.debug("Did not evolve schema, version already exists as 2")
        }
        verify(exactly = 0) { client.updateCompatibility(subject) }
    }

    @Test
    fun `can update compatibility and evolve schema`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()
        val subject = Subject("foo", Compatibility.FORWARD_TRANSITIVE, schema)

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(subject)

        every { client.subjects() } returns listOf("foo")
        every { client.version(subject) } returns null
        every { client.compatibility(subject.name) } returns Compatibility.FULL
        every { client.updateCompatibility(subject) } returns Compatibility.FORWARD_TRANSITIVE
        every { client.evolve(subject) } returns 2

        stateApplier.apply(state)

        verifyOrder {
            client.updateCompatibility(subject)
            logger.info("Changed 'foo' compatibility to FORWARD_TRANSITIVE")
            client.version(subject)
            client.evolve(subject)
            logger.info("Evolved existing schema for subject 'foo' to version 2")
        }
    }

    @Test
    fun `will not change subject compatibility if matches state`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()
        val subject = Subject("foo", Compatibility.FULL, schema)

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(subject)

        every { client.subjects() } returns listOf("foo")
        every { client.version(subject) } returns 1
        every { client.compatibility("foo") } returns Compatibility.FULL

        stateApplier.apply(state)

        verifyOrder {
            client.subjects()
            client.compatibility("foo")
            logger.debug("Did not change compatibility level for 'foo' as it matched desired level FULL")
            client.version(subject)
            logger.debug("Did not evolve schema, version already exists as 1")
        }
        verify(exactly = 0) {
            client.updateCompatibility(subject)
        }
    }
}
