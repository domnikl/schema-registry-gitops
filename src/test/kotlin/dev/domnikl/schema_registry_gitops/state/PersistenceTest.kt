package dev.domnikl.schema_registry_gitops.state

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaParseException
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import dev.domnikl.schema_registry_gitops.avroFromResources
import dev.domnikl.schema_registry_gitops.fromResources
import dev.domnikl.schema_registry_gitops.stringFromResources
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference
import io.confluent.kafka.schemaregistry.json.JsonSchema
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.Logger
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.Optional

class PersistenceTest {
    private val logger = mockk<Logger>(relaxed = true)
    private val schemaRegistryClient = mockk<CachedSchemaRegistryClient>()
    private val loader = Persistence(schemaRegistryClient, logger)

    @Test
    fun `throws exception when trying to load empty file`() {
        assertThrows<IllegalArgumentException> {
            loader.load(fromResources("schemas"), fromResources("empty.yml"))
        }
    }

    @Test
    fun `throws exception when trying to load non-existing file`() {
        assertThrows<IllegalArgumentException> {
            loader.load(fromResources("schemas"), File("foo"))
        }
    }

    @Test
    fun `can load file with only compatibility`() {
        val state = loader.load(fromResources("schemas"), fromResources("only_compatibility.yml"))

        assertEquals(Compatibility.FORWARD, state.compatibility)
        assertEquals(emptyList<Subject>(), state.subjects)
    }

    @Test
    fun `debug logs which file it loads`() {
        val basePath = fromResources("schemas")
        val file = fromResources("no_compatibility.yml")

        loader.load(basePath, file)

        verify { logger.debug("Loading state file ${file.absolutePath}, referenced schemas from ${basePath.absolutePath}") }
    }

    @Test
    fun `can load file without subjects`() {
        val state = loader.load(fromResources("schemas"), fromResources("no_compatibility.yml"))

        assertNull(state.compatibility)
        assertEquals(emptyList<Subject>(), state.subjects)
    }

    @Test
    fun `can load file with subjects`() {
        val schemaString = stringFromResources("schemas/with_subjects.avsc")
        val schema = mockk<ParsedSchema>()

        every { schemaRegistryClient.parseSchema("AVRO", schemaString, emptyList()) } returns Optional.of(schema)

        val state = loader.load(fromResources("schemas"), fromResources("with_subjects.yml"))

        assertNull(state.compatibility)
        assertEquals(listOf(Subject("foo", null, schema)), state.subjects)
    }

    @Test
    fun `can load file with subjects and references`() {
        val schemaString = stringFromResources("schemas/with_subjects_and_references.avsc")
        val schema = mockk<ParsedSchema>()

        every { schemaRegistryClient.parseSchema("AVRO", schemaString, listOf(SchemaReference("dev.domnikl.schema_registry_gitops.foo", "foo", 1))) } returns Optional.of(schema)

        val state = loader.load(fromResources("schemas"), fromResources("with_subjects_and_references.yml"))

        assertNull(state.compatibility)
        assertEquals(listOf(Subject("bar", null, schema, listOf(SchemaReference("dev.domnikl.schema_registry_gitops.foo", "foo", 1)))), state.subjects)
    }

    @Test
    fun `can load file with inline schema`() {
        val schemaString =
            """
                {
                   "type": "record",
                   "name": "HelloWorld",
                   "namespace": "dev.domnikl.schema_registry_gitops",
                   "fields": [
                     {
                       "name": "greeting",
                       "type": "string"
                     }
                   ]
                }
            """
        val schema = mockk<ParsedSchema>()

        every {
            schemaRegistryClient.parseSchema(
                "AVRO",
                match { it.replace("\\s".toRegex(), "") == schemaString.replace("\\s".toRegex(), "") },
                emptyList()
            )
        } returns Optional.of(schema)

        val state = loader.load(fromResources("schemas"), fromResources("with_inline_schema.yml"))

        assertEquals(listOf(Subject("foo", Compatibility.BACKWARD, schema)), state.subjects)
    }

    @Test
    fun `can load file with subjects and compatibility`() {
        val schemaString = stringFromResources("schemas/with_subjects.avsc")
        val schema = mockk<ParsedSchema>()

        every { schemaRegistryClient.parseSchema("AVRO", schemaString, emptyList()) } returns Optional.of(schema)

        val state = loader.load(fromResources("schemas"), fromResources("with_subjects_and_compatibility.yml"))

        assertEquals(Compatibility.FULL, state.compatibility)
        assertEquals(listOf(Subject("foo", Compatibility.FORWARD, schema)), state.subjects)
    }

    @Test
    fun `throws exception when neither schema nor file was given`() {
        assertThrows<IllegalArgumentException> {
            loader.load(fromResources("schemas"), fromResources("neither_schema_nor_file.yml"))
        }
    }

    @Test
    fun `throws exception when referenced file does not exist`() {
        assertThrows<FileNotFoundException> {
            loader.load(fromResources("schemas"), fromResources("referenced_file_does_not_exist.yml"))
        }
    }

    @Test
    fun `throws exception when subject name is missing`() {
        assertThrows<MissingKotlinParameterException> {
            loader.load(fromResources("schemas"), fromResources("subject_name_is_missing.yml"))
        }
    }

    @Test
    fun `can save state to a file`() {
        val schema1 = avroFromResources("schemas/with_subjects.avsc")
        val schema2 = avroFromResources("schemas/key.avsc")

        every { schemaRegistryClient.parseSchema("AVRO", schema1.toString(), emptyList()) } returns Optional.of(schema1)
        every { schemaRegistryClient.parseSchema("AVRO", schema2.toString(), emptyList()) } returns Optional.of(schema2)

        val tempFile = File.createTempFile(javaClass.simpleName, "can-save-state-to-a-file")
        tempFile.deleteOnExit()

        val outputStream = BufferedOutputStream(FileOutputStream(tempFile))

        val currentState = State(
            Compatibility.BACKWARD_TRANSITIVE,
            listOf(
                Subject("foobar-value", null, schema1),
                Subject("foobar-key", Compatibility.FULL, schema2)
            )
        )

        loader.save(currentState, outputStream)

        assertEquals(currentState, loader.load(fromResources("schemas"), tempFile))
    }

    @Test
    fun `can save state to a file without global compatibility`() {
        val outputStream = ByteArrayOutputStream()

        val currentState = State(
            null,
            emptyList()
        )

        loader.save(currentState, outputStream)

        val expectedOutput =
            """
                compatibility: NONE
                subjects: []

            """.trimIndent()

        assertEquals(expectedOutput, outputStream.toString())
    }

    class YamlSubjectTest {
        private val schemaRegistryClient = CachedSchemaRegistryClient(
            listOf("http://foo.bar"),
            100,
            listOf(AvroSchemaProvider(), ProtobufSchemaProvider(), JsonSchemaProvider()),
            null,
            null
        )

        @Test
        fun `file takes precedence over inline schema`() {
            val subject = Persistence.YamlSubject(
                "foo",
                "key.avsc",
                "AVRO",
                """
                    syntax = "proto3";
                    package com.acme;

                    message OtherRecord {
                      int32 an_id = 1;
                    }
                """.trimIndent(),
                null
            )

            val schema = subject.parseSchema(fromResources("schemas"), schemaRegistryClient)

            assert(schema is AvroSchema)
        }

        @Test
        fun `can parse Protobuf schema`() {
            val subject = Persistence.YamlSubject(
                "foo",
                null,
                "PROTOBUF",
                """
                    syntax = "proto3";
                    package com.acme;

                    message OtherRecord {
                      int32 an_id = 1;
                    }

                """.trimIndent(),
                null
            )

            val schema = subject.parseSchema(File("."), schemaRegistryClient)

            assert(schema is ProtobufSchema)
        }

        @Test
        fun `can parse Avro schema`() {
            val subject = Persistence.YamlSubject(
                "foo",
                fromResources("schemas/key.avsc").name,
                "AVRO",
                null,
                null
            )

            val schema = subject.parseSchema(fromResources("schemas"), schemaRegistryClient)

            assert(schema is AvroSchema)
        }

        @Test
        fun `can parse JSON schema`() {
            val subject = Persistence.YamlSubject(
                "foo",
                null,
                "JSON",
                """
                    {
                      "type": "object",
                      "properties": {
                        "f1": {
                          "type": "string"
                        }
                      }
                    }

                """.trimIndent(),
                null
            )

            val schema = subject.parseSchema(File("."), schemaRegistryClient)

            assert(schema is JsonSchema)
        }

        @Test
        fun `throws exception when neither file nor schema is set`() {
            val subject = Persistence.YamlSubject(
                "foo",
                null,
                "JSON",
                null,
                null
            )

            assertThrows<IllegalArgumentException> {
                subject.parseSchema(File("."), schemaRegistryClient)
            }
        }

        @Test
        fun `throws exception when schema could not be parsed`() {
            val subject = Persistence.YamlSubject(
                "foo",
                null,
                "JSON",
                "foobar",
                null
            )

            assertThrows<SchemaParseException> {
                subject.parseSchema(File("."), schemaRegistryClient)
            }
        }
    }
}
