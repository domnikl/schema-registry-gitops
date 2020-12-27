package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class StateValidatorTest {
    private val client = mockk<SchemaRegistryClient>()
    private val validator = StateValidator(client)

    @Test
    fun `returns empty list if all subjects are new`() {
        every { client.allSubjects } returns emptyList()

        val state = State(
            Compatibility.FULL,
            listOf(
                Subject(
                    "foo",
                    null,
                    schema = mockk()
                )
            )
        )

        assertEquals(emptyList<String>(), validator.validate(state))
    }

    @Test
    fun `returns empty list if all schemas are valid`() {
        val schema = mockk<ParsedSchema>()

        every { client.allSubjects } returns listOf("foo", "bar")
        every { client.testCompatibility("foo", schema) } returns true
        every { client.testCompatibility("bar", schema) } returns true

        val state = State(
            Compatibility.FULL,
            listOf(
                Subject(
                    "foo",
                    null,
                    schema
                ),
                Subject(
                    "bar",
                    null,
                    schema
                )
            )
        )

        assertEquals(emptyList<String>(), validator.validate(state))
    }

    @Test
    fun `returns subject name if schema is not compatible with an earlier version`() {
        val schema = mockk<ParsedSchema>()

        every { client.allSubjects } returns listOf("foo", "bar")
        every { client.testCompatibility("foo", schema) } returns false
        every { client.testCompatibility("bar", schema) } returns true

        val state = State(
            Compatibility.FULL,
            listOf(
                Subject(
                    "foo",
                    null,
                    schema
                ),
                Subject(
                    "bar",
                    null,
                    schema
                )
            )
        )

        assertEquals(listOf("foo"), validator.validate(state))
    }
}
