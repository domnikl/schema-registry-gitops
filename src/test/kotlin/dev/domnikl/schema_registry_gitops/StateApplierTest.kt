package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.ConfigUpdateRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class StateApplierTest {
    private val restService = mockk<RestService>()
    private val client = mockk<SchemaRegistryClient>()
    private val stateApplier = StateApplier(restService, client)

    @Test
    fun `can apply compatibility`() {
        val state = mockk<State>()

        every { state.compatibility } returns Compatibility.FULL_TRANSITIVE
        every { state.subjects } returns emptyList()

        val request = mockk<ConfigUpdateRequest>()

        every { restService.updateCompatibility("FULL_TRANSITIVE", "") } returns request

        stateApplier.apply(state)

        verify { restService.updateCompatibility("FULL_TRANSITIVE", "") }
    }

    @Test
    fun `can register subject`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(
            Subject("foo", null, schema)
        )

        every { client.register("foo", schema) } returns 1

        stateApplier.apply(state)

        verify { client.register("foo", schema) }
        verify(exactly = 0) { restService.updateCompatibility(any(), any()) }
    }

    @Test
    fun `can update subject compatibility`() {
        val schema = schemaFromResources("schemas/key.avsc")
        val state = mockk<State>()

        every { state.compatibility } returns null
        every { state.subjects } returns listOf(
            Subject("foo", Compatibility.BACKWARD, schema)
        )

        every { client.register("foo", schema) } returns 2
        every { client.updateCompatibility("foo", "BACKWARD") } returns "BACKWARD"

        stateApplier.apply(state)

        verify { client.register("foo", schema) }
        verify { client.updateCompatibility("foo", "BACKWARD") }
        verify(exactly = 0) { restService.updateCompatibility(any(), "") }
    }
}
