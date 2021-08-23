package dev.domnikl.schema_registry_gitops

import org.junit.Assert.assertEquals
import org.junit.Test

class ParsedSchemaTest {
    @Test
    fun `can get deltas for AVRO`() {
        val a = avroFromResources("schemas/deltaA.avsc")
        val b = avroFromResources("schemas/deltaB.avsc")

        val diff = a.diff(b)

        assertEquals(stringFromResources("schemas/avsc.diff"), diff)
    }

    @Test
    fun `can get deltas for Protobuf`() {
        val a = protoFromResources("schemas/deltaA.proto")
        val b = protoFromResources("schemas/deltaB.proto")

        val diff = a.diff(b)

        assertEquals(stringFromResources("schemas/proto.diff"), diff)
    }

    @Test
    fun `can get deltas for JSON Schema`() {
        val a = jsonFromResources("schemas/deltaA.json")
        val b = jsonFromResources("schemas/deltaB.json")

        val diff = a.diff(b)

        assertEquals(stringFromResources("schemas/json.diff"), diff)
    }
}
