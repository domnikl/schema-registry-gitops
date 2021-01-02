package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class DumperTest {
    private val client = mockk<SchemaRegistryClient>()
    private val stateDumper = Dumper(client)

    @Test
    fun `can dump current state with subjects`() {
        val schema = "{\"name\":\"FooKey\",\"type\":\"string\"}"

        every { client.getCompatibility("") } returns "FULL"
        every { client.allSubjects } returns listOf("foo", "bar")

        every { client.getCompatibility("foo") } returns "FULL_TRANSITIVE"
        every { client.getLatestSchemaMetadata("foo").schema } returns schema

        every { client.getCompatibility("bar") } returns "BACKWARD"
        every { client.getLatestSchemaMetadata("bar").schema } returns schema

        val expectedState = State(
            Compatibility.FULL,
            listOf(
                Subject(
                    "foo",
                    Compatibility.FULL_TRANSITIVE,
                    AvroSchema(schema)
                ),
                Subject(
                    "bar",
                    Compatibility.BACKWARD,
                    AvroSchema(schema)
                )
            )
        )

        val state = stateDumper.dump()

        assertEquals(expectedState, state)
    }

    @Test
    fun `can dump current state handling implicit compatibility`() {
        val schema = "{\"name\":\"FooKey\",\"type\":\"string\"}"

        every { client.getCompatibility("") } returns "FULL"
        every { client.allSubjects } returns listOf("bar")

        every { client.getCompatibility("bar") } returns "NONE"
        every { client.getLatestSchemaMetadata("bar").schema } returns schema

        val expectedState = State(
            Compatibility.FULL,
            listOf(
                Subject(
                    "bar",
                    Compatibility.NONE,
                    AvroSchema(schema)
                )
            )
        )

        val state = stateDumper.dump()

        assertEquals(expectedState, state)
    }
}
