package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DiffingTest {
    private val client = mockk<SchemaRegistryClient>(relaxed = true)
    private val diff = Diffing(client)
    private val schema = AvroSchema("{\"type\": \"record\",\"name\": \"HelloWorld\",\"namespace\": \"dev.domnikl.schema_registry_gitops\",\"doc\": \"this is some docs to be replaced ...\",\"fields\": [{\"name\": \"greeting\",\"type\": \"string\"}]}")
    private val subject = Subject("foobar", Compatibility.FORWARD, schema)
    private val subject2 = Subject("bar", Compatibility.BACKWARD, schema)

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
        every { remoteSchema.canonicalString() } returns subject.schema.canonicalString()
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
    fun `can detect that schema has been modified for subject`() {
        val state = State(Compatibility.BACKWARD, listOf(subject, subject2))
        val remoteSchema = mockk<ParsedSchema>()

        every { client.subjects() } returns listOf("foobar", "bar")
        every { remoteSchema.canonicalString() } returns subject.schema.canonicalString()
        every { client.getLatestSchema("foobar") } returns remoteSchema
        every { client.getLatestSchema("bar") } returns remoteSchema
        every { client.version(subject) } returns null
        every { client.version(subject2) } returns null
        every { client.testCompatibility(any()) } returns true
        every { client.compatibility("foobar") } returns subject.compatibility!!
        every { client.compatibility("bar") } returns subject2.compatibility!!

        val result = diff.diff(state)

        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Subject>(), result.deleted)
        assertEquals(
            listOf(
                Diffing.Changes(subject, null, Diffing.Change(remoteSchema, subject.schema)),
                Diffing.Changes(subject2, null, Diffing.Change(remoteSchema, subject.schema)),
            ),
            result.modified
        )
    }

    @Test
    fun `can detect that schema already exists in an older version`() {
        val state = State(Compatibility.BACKWARD, listOf(subject))
        val remoteSchema = mockk<ParsedSchema>()

        every { client.subjects() } returns listOf("foobar")
        every { remoteSchema.canonicalString() } returns subject.schema.canonicalString()
        every { client.getLatestSchema("foobar") } returns remoteSchema
        every { client.version(subject) } returns 5
        every { client.testCompatibility(any()) } returns true
        every { client.compatibility("foobar") } returns subject.compatibility!!

        val result = diff.diff(state)

        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Subject>(), result.deleted)
        assertEquals(emptyList<Diffing.Changes>(), result.modified)
    }

    @Test
    fun `can detect that nothing has changed`() {
        val state = State(Compatibility.BACKWARD, listOf(subject))
        val remoteSchema = mockk<ParsedSchema>()

        every { client.subjects() } returns listOf("foobar")
        every { remoteSchema.canonicalString() } returns subject.schema.canonicalString()
        every { client.getLatestSchema("foobar") } returns remoteSchema
        every { client.testCompatibility(any()) } returns true
        every { client.compatibility("foobar") } returns subject.compatibility!!

        val result = diff.diff(state)

        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Subject>(), result.deleted)
        assertEquals(emptyList<Diffing.Changes>(), result.modified)
    }

    @Test
    fun `can detect doc changes`() {
        val changedSchema = AvroSchema("{\"type\": \"record\",\"name\": \"HelloWorld\",\"namespace\": \"dev.domnikl.schema_registry_gitops\",\"doc\": \"This is the new docs.\",\"fields\": [{\"name\": \"greeting\",\"type\": \"string\"}]}")

        val subject = Subject("foobar", Compatibility.FORWARD, changedSchema)
        val state = State(Compatibility.BACKWARD, listOf(subject))

        every { client.subjects() } returns listOf("foobar")
        every { client.getLatestSchema("foobar") } returns schema
        every { client.testCompatibility(any()) } returns true
        every { client.compatibility("foobar") } returns subject.compatibility!!

        val result = diff.diff(state)

        assertEquals(emptyList<Subject>(), result.incompatible)
        assertEquals(emptyList<Subject>(), result.added)
        assertEquals(emptyList<Subject>(), result.deleted)
        assertEquals(listOf(Diffing.Changes(subject, null, Diffing.Change(schema, changedSchema))), result.modified)
    }

    class ResultTest {
        @Test
        fun `can be check if empty`() {
            assert(Diffing.Result().isEmpty())
            assertFalse(Diffing.Result(incompatible = listOf(mockk())).isEmpty())
            assertFalse(Diffing.Result(added = listOf(mockk())).isEmpty())
            assertFalse(Diffing.Result(modified = listOf(mockk())).isEmpty())
            assertFalse(Diffing.Result(compatibility = Diffing.Change(Compatibility.NONE, Compatibility.FULL)).isEmpty())
            assertFalse(Diffing.Result(deleted = listOf("foo")).isEmpty())
        }
    }
}
