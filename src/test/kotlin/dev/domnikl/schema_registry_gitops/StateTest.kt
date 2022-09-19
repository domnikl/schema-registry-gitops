package dev.domnikl.schema_registry_gitops

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
        val a = State(
            null,
            listOf(
                Subject("abc", null, mockk()),
                Subject("foo", Compatibility.BACKWARD, mockk())
            )
        )

        val b = State(
            Compatibility.BACKWARD,
            listOf(
                Subject("foo", null, mockk()),
                Subject("bar", null, mockk())
            )
        )

        val merged = a.merge(b)

        assertEquals(listOf("abc", "foo", "bar"), merged.subjects.map { it.name })
        assertEquals(listOf(null, null, null), merged.subjects.map { it.compatibility })
    }
}
