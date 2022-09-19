package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows

class StateTest {
    @Test
    fun `validates uniqueness of subjects`() {
        assertThrows<IllegalArgumentException> {
            State(
                null,
                listOf(
                    Subject("foo", null, mockk()),
                    Subject("foo", null, mockk())
                )
            )
        }
    }

    @Test
    fun `can merge two states`() {
        val schema1 = mockk<ParsedSchema>()
        val schema2 = mockk<ParsedSchema>()
        val schema3 = mockk<ParsedSchema>()
        val schema4 = mockk<ParsedSchema>()

        val a = State(
            null,
            listOf(
                Subject("abc", null, schema1),
                Subject("foo", Compatibility.BACKWARD, schema2)
            )
        )

        val b = State(
            Compatibility.BACKWARD,
            listOf(
                Subject("foo", null, schema3),
                Subject("bar", null, schema4)
            )
        )

        val merged = a.merge(b)

        assertEquals(Compatibility.BACKWARD, merged.compatibility)
        assertEquals(listOf("abc", "foo", "bar"), merged.subjects.map { it.name })
        assertEquals(listOf(null, null, null), merged.subjects.map { it.compatibility })
        assertEquals(listOf(schema1, schema3, schema4), merged.subjects.map { it.schema })
    }

    @Test
    fun `can merge and keep latest global compatibility`() {
        val a = State(Compatibility.FORWARD, emptyList())
        val b = State(Compatibility.BACKWARD, emptyList())

        assertEquals(Compatibility.BACKWARD, a.merge(b).compatibility)
        assertEquals(Compatibility.FORWARD, b.merge(a).compatibility)
    }
}
