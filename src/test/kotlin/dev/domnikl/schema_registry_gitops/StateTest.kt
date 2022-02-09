package dev.domnikl.schema_registry_gitops

import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class StateTest {
    @Test
    fun `validates uniqueness of subjects`() {
        assertThrows<IllegalArgumentException> {
            State(
                null,
                listOf(
                    Subject("foo", null, mockk(), null),
                    Subject("foo", null, mockk(), null)
                )
            )
        }
    }
}
