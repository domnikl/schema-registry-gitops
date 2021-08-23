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

class DiffingTest {
    private val client = mockk<SchemaRegistryClient>(relaxed = true)
    private val diff = Diffing(client)
    private val schema = mockk<ParsedSchema>()

    private val subject = Subject("foobar", Compatibility.FORWARD, schema)

    @Test
    fun `can detect global compatibility change`() {
        val state = State(Compatibility.BACKWARD, emptyList())

        every { client.globalCompatibility() } returns Compatibility.NONE

        val result = diff.diff(state)

        assertEquals(Diffing.Change(Compatibility.NONE, Compatibility.BACKWARD), result.compatibility)
        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Subject>(), result.deleted)
        assertEquals(emptyList<Diffing.Changes>(), result.modified)
    }

    @Test
    fun `can detect incompatible changes`() {
        val state = State(Compatibility.BACKWARD, listOf(subject))

        every { client.subjects() } returns listOf("foobar")
        every { client.testCompatibility(any()) } returns false

        val result = diff.diff(state)

        assertEquals(listOf(subject), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Diffing.Changes>(), result.modified)
        assertEquals(emptyList<String>(), result.deleted)
    }

    @Test
    fun `can detect added subjects`() {
        val state = State(Compatibility.BACKWARD, listOf(subject))

        every { client.subjects() } returns emptyList()
        every { client.testCompatibility(any()) } returns true

        val result = diff.diff(state)

        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(listOf(subject), result.added)
        assertEquals(emptyList<Diffing.Changes>(), result.modified)
        assertEquals(emptyList<String>(), result.deleted)
    }

    @Test
    fun `can detect deleted subjects`() {
        val state = State(Compatibility.BACKWARD, emptyList())

        every { client.subjects() } returns listOf("foobar")

        val result = diff.diff(state, true)

        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Diffing.Changes>(), result.modified)
        assertEquals(listOf("foobar"), result.deleted)
    }

    @Test
    fun `will hide deletes if not enabled`() {
        val state = State(Compatibility.BACKWARD, emptyList())

        every { client.subjects() } returns listOf("foobar")

        val result = diff.diff(state, false)

        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Diffing.Changes>(), result.modified)
        assertEquals(emptyList<String>(), result.deleted)
    }

    @Test
    fun `can detect if compatibility has been modified for subjects`() {
        val state = State(Compatibility.BACKWARD, listOf(subject))
        val remoteSchema = mockk<ParsedSchema>()

        every { client.subjects() } returns listOf("foobar")
        every { remoteSchema.deepEquals(any()) } returns true
        every { client.getLatestSchema("foobar") } returns remoteSchema
        every { client.testCompatibility(any()) } returns true
        every { client.compatibility("foobar") } returns Compatibility.BACKWARD_TRANSITIVE

        val result = diff.diff(state)

        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Subject>(), result.deleted)
        assertEquals(1, result.modified.size)
        assertEquals(
            Diffing.Changes(
                subject,
                Diffing.Change(Compatibility.BACKWARD_TRANSITIVE, Compatibility.FORWARD),
                null
            ),
            result.modified.first()
        )
    }

    @Test
    fun `can detect if schema has been modified for subjects`() {
        val state = State(Compatibility.BACKWARD, listOf(subject))
        val remoteSchema = mockk<ParsedSchema>()

        every { client.subjects() } returns listOf("foobar")
        every { remoteSchema.deepEquals(any()) } returns false
        every { client.getLatestSchema("foobar") } returns remoteSchema
        every { client.testCompatibility(any()) } returns true
        every { client.compatibility("foobar") } returns subject.compatibility!!

        val result = diff.diff(state)

        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Subject>(), result.deleted)
        assertEquals(1, result.modified.size)
        assertEquals(
            Diffing.Changes(subject, null, Diffing.Change(remoteSchema, subject.schema)),
            result.modified.first()
        )
    }

    @Test
    fun `can detect that nothing has changed`() {
        val state = State(Compatibility.BACKWARD, listOf(subject))
        val remoteSchema = mockk<ParsedSchema>()

        every { client.subjects() } returns listOf("foobar")
        every { remoteSchema.deepEquals(any()) } returns true
        every { client.getLatestSchema("foobar") } returns remoteSchema
        every { client.testCompatibility(any()) } returns true
        every { client.compatibility("foobar") } returns subject.compatibility!!

        val result = diff.diff(state)

        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Subject>(), result.deleted)
        assertEquals(emptyList<Diffing.Changes>(), result.modified)
    }
}
