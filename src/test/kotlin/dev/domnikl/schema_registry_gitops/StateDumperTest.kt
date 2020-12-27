package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.rest.RestService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class StateDumperTest {
    private val restService = mockk<RestService>()
    private val stateDumper = StateDumper(restService)

    @Test
    fun `can dump current state with subjects`() {
        val schema = "{\"name\":\"FooKey\",\"type\":\"string\"}"

        every { restService.getConfig("").compatibilityLevel } returns "FULL"
        every { restService.allSubjects } returns listOf("foo", "bar")

        every { restService.getConfig("foo").compatibilityLevel } returns "FULL_TRANSITIVE"
        every { restService.getLatestVersion("foo").schema } returns schema

        every { restService.getConfig("bar").compatibilityLevel } returns "BACKWARD"
        every { restService.getLatestVersion("bar").schema } returns schema

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

        every { restService.getConfig("").compatibilityLevel } returns "FULL"
        every { restService.allSubjects } returns listOf("bar")

        every { restService.getConfig("bar").compatibilityLevel } returns "NONE"
        every { restService.getLatestVersion("bar").schema } returns schema

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
