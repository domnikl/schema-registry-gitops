package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidatorTest {
    private val client = mockk<SchemaRegistryClient>()
    private val validator = Validator(client)

    @Test
    fun `returns empty list if all subjects are compatible`() {
        every { client.testCompatibility(any()) } returns true

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
        val subjectFoo = Subject("foo", null, schema)
        val subjectBar = Subject("bar", null, schema)

        every { client.testCompatibility(subjectFoo) } returns true
        every { client.testCompatibility(subjectBar) } returns true

        val state = State(
            Compatibility.FULL,
            listOf(
                subjectFoo,
                subjectBar
            )
        )

        assertEquals(emptyList<String>(), validator.validate(state))
    }

    @Test
    fun `returns subject name if schema is not compatible with an earlier version`() {
        val schema = mockk<ParsedSchema>()
        val subjectFoo = Subject("foo", null, schema)
        val subjectBar = Subject("bar", null, schema)

        every { client.subjects() } returns listOf("foo", "bar")
        every { client.testCompatibility(subjectFoo) } returns false
        every { client.testCompatibility(subjectBar) } returns true

        val state = State(
            Compatibility.FULL,
            listOf(
                subjectFoo,
                subjectBar
            )
        )

        assertEquals(listOf("foo"), validator.validate(state))
    }
}
