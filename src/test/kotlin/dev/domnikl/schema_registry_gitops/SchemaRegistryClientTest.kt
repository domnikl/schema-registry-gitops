package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.SchemaMetadata
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient as WrappedSchemaRegistryClient

class SchemaRegistryClientTest {
    private val client = mockk<WrappedSchemaRegistryClient>()
    private val wrapper = SchemaRegistryClient(client)

    @Test
    fun `can get subjects`() {
        every { client.allSubjects } returns mutableListOf("foo", "bar")

        assertEquals(listOf("foo", "bar"), wrapper.subjects())
    }

    @Test
    fun `can get global compatibility`() {
        every { client.getCompatibility("") } returns "BACKWARD"

        assertEquals(Compatibility.BACKWARD, wrapper.globalCompatibility())
    }

    @Test
    fun `can update global compatibility`() {
        every { client.updateCompatibility("", "FULL") } returns "FULL"

        assertEquals(
            Compatibility.FULL,
            wrapper.updateGlobalCompatibility(Compatibility.FULL)
        )
    }

    @Test
    fun `can get compatibility`() {
        every { client.getCompatibility("foo") } returns "FORWARD"

        assertEquals(
            Compatibility.FORWARD,
            wrapper.compatibility("foo")
        )
    }

    @Test
    fun `can get compatibility if none is set`() {
        every { client.getCompatibility("foo") } throws RestClientException("", 404, 40403)

        assertEquals(
            Compatibility.NONE,
            wrapper.compatibility("foo")
        )
    }

    @Test
    fun `rethrows RestClientExceptionIfAnotherErrorOccurred`() {
        every { client.getCompatibility("foo") } throws RestClientException("", 404, 50001)

        assertThrows<RestClientException> {
            wrapper.compatibility("foo")
        }
    }

    @Test
    fun `can update compatibility`() {
        val subject = Subject("foo", Compatibility.FORWARD, mockk())

        every { client.updateCompatibility("foo", "FORWARD") } returns "NONE"

        assertEquals(
            Compatibility.NONE,
            wrapper.updateCompatibility(subject)
        )
    }

    @Test
    fun `can test compatibility`() {
        val schema = mockk<ParsedSchema>()
        val subject = Subject("foo", Compatibility.FORWARD, schema)

        every { client.testCompatibility("foo", schema) } returns true

        assert(wrapper.testCompatibility(subject))
    }

    @Test
    fun `test compatibility returns true if subject is new`() {
        val schema = mockk<ParsedSchema>()
        val subject = Subject("foo", null, schema)

        every { client.testCompatibility("foo", schema) } throws RestClientException("", 404, 40401)

        assert(wrapper.testCompatibility(subject))
    }

    @Test
    fun `test compatibility returns true if version is new`() {
        val schema = mockk<ParsedSchema>()
        val subject = Subject("foo", null, schema)

        every { client.testCompatibility("foo", schema) } throws RestClientException("", 404, 40402)

        assert(wrapper.testCompatibility(subject))
    }

    @Test
    fun `test compatibility returns true if schema is new`() {
        val schema = mockk<ParsedSchema>()
        val subject = Subject("foo", null, schema)

        every { client.testCompatibility("foo", schema) } throws RestClientException("", 404, 40403)

        assert(wrapper.testCompatibility(subject))
    }

    @Test
    fun `test compatibility returns false if client returns false`() {
        val schema = mockk<ParsedSchema>()
        val subject = Subject("foo", null, schema)

        every { client.testCompatibility("foo", schema) } returns false

        assertFalse(wrapper.testCompatibility(subject))
    }

    @Test
    fun `test compatibility throws exception if schema type is not supported`() {
        val schema = mockk<ParsedSchema>()
        val subject = Subject("foo", null, schema)

        every { client.testCompatibility("foo", schema) } throws RestClientException("", 422, 422)

        assertThrows<ServerVersionMismatchException> {
            wrapper.testCompatibility(subject)
        }
    }

    @Test
    fun `can get latest schema`() {
        val schema = avroFromResources("schemas/key.avsc")
        val schemaMetadata = SchemaMetadata(42, 1, "AVRO", emptyList(), schema.toString())
        val optionalSchema: Optional<ParsedSchema> = Optional.of(schema)

        every { client.parseSchema("AVRO", schema.toString(), emptyList()) } returns optionalSchema
        every { client.getLatestSchemaMetadata("foo") } returns schemaMetadata

        assertEquals(schema, wrapper.getLatestSchema("foo"))
    }

    @Test
    fun `can create subject`() {
        val schema = mockk<ParsedSchema>()
        val subject = Subject("foo", Compatibility.FORWARD, schema)

        every { client.register("foo", schema) } returns 42

        assertEquals(42, wrapper.create(subject))
    }

    @Test
    fun `can delete subject`() {
        every { client.deleteSubject("foo") } returns listOf(1, 2)

        wrapper.delete("foo")

        verify {
            client.deleteSubject("foo")
        }
    }

    @Test
    fun `create will throw exception when schemaType is used prior to server version 55`() {
        val schema = mockk<ParsedSchema>()
        val subject = Subject("foo", Compatibility.FORWARD, schema)

        every { client.register("foo", schema) } throws RestClientException("", 422, 422)

        assertThrows<ServerVersionMismatchException> {
            wrapper.create(subject)
        }
    }

    @Test
    fun `can evolve schema for subject`() {
        val schema = mockk<ParsedSchema>()
        val subject = Subject("foo", Compatibility.FORWARD, schema)

        every { client.register("foo", schema) } returns 21

        assertEquals(21, wrapper.evolve(subject))
    }

    @Test
    fun `can get version for schema`() {
        val subject = Subject("foo", Compatibility.FORWARD, mockk())

        every { client.getVersion("foo", subject.schema) } returns 21

        assertEquals(21, wrapper.version(subject))
    }

    @Test
    fun `version returns null if it is not registered yet`() {
        val subject = Subject("foo", Compatibility.FORWARD, mockk())

        every { client.getVersion("foo", subject.schema) } throws RestClientException("", 404, 40403)

        assertNull(wrapper.version(subject))
    }
}
