package dev.domnikl.schema_registry_gitops

import io.mockk.mockk
import org.junit.Assert.assertThrows
import org.junit.Test

class StateTest {
    @Test
    fun `validates uniqueness of subjects`() {
        assertThrows(IllegalArgumentException::class.java) {
            State(
                null,
                listOf(
                    Subject("foo", null, mockk()),
                    Subject("foo", null, mockk())
                )
            )
        }
    }
}
