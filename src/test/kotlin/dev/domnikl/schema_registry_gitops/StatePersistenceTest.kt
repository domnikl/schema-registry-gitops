package dev.domnikl.schema_registry_gitops

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import org.apache.avro.Schema
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException

class StatePersistenceTest {
    private val loader = StatePersistence(fromResources("schemas"))

    @Test
    fun `throws exception when trying to load empty file`() {
        assertThrows(IllegalArgumentException::class.java) {
            loader.load(fromResources("empty.yml"))
        }
    }

    @Test
    fun `throws exception when trying to load non-existing file`() {
        assertThrows(IllegalArgumentException::class.java) {
            loader.load(File("foo"))
        }
    }

    @Test
    fun `can load file with only compatiblity`() {
        val state = loader.load(fromResources("only_compatibility.yml"))

        assertEquals(Compatibility.FORWARD, state.compatibility)
        assertEquals(emptyList<Subject>(), state.subjects)
    }

    @Test
    fun `can load file without subjects`() {
        val state = loader.load(fromResources("no_compatibility.yml"))

        assertNull(state.compatibility)
        assertEquals(emptyList<Subject>(), state.subjects)
    }

    @Test
    fun `can load file with subjects`() {
        val schema = AvroSchema(Schema.Parser().parse(fromResources("schemas/with_subjects.avsc")))
        val state = loader.load(fromResources("with_subjects.yml"))

        assertNull(state.compatibility)
        assertEquals(listOf(Subject("foo", null, schema)), state.subjects)
    }

    @Test
    fun `can load file with inline schema`() {
        val schema = AvroSchema(Schema.Parser().parse(fromResources("schemas/with_subjects.avsc")))
        val state = loader.load(fromResources("with_inline_schema.yml"))

        assertEquals(listOf(Subject("foo", Compatibility.BACKWARD, schema)), state.subjects)
    }

    @Test
    fun `can load file with subjects and compatibility`() {
        val schema = AvroSchema(Schema.Parser().parse(fromResources("schemas/with_subjects.avsc")))
        val state = loader.load(fromResources("with_subjects_and_compatibility.yml"))

        assertEquals(Compatibility.FULL, state.compatibility)
        assertEquals(listOf(Subject("foo", Compatibility.FORWARD, schema)), state.subjects)
    }

    @Test
    fun `throws exception when neither schema nor file was given`() {
        assertThrows(IllegalArgumentException::class.java) {
            loader.load(fromResources("neither_schema_nor_file.yml"))
        }
    }

    @Test
    fun `throws exception when referenced file does not exist`() {
        assertThrows(FileNotFoundException::class.java) {
            loader.load(fromResources("referenced_file_does_not_exist.yml"))
        }
    }

    @Test
    fun `throws exception when subject name is missing`() {
        assertThrows(MissingKotlinParameterException::class.java) {
            loader.load(fromResources("subject_name_is_missing.yml"))
        }
    }

    private fun fromResources(name: String): File {
        return File(javaClass.classLoader.getResource(name)!!.toURI())
    }
}
