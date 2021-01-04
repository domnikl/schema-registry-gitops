package dev.domnikl.schema_registry_gitops.state

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import dev.domnikl.schema_registry_gitops.fromResources
import dev.domnikl.schema_registry_gitops.schemaFromResources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class PersistenceTest {
    private val loader = Persistence()

    @Test
    fun `throws exception when trying to load empty file`() {
        assertThrows(IllegalArgumentException::class.java) {
            loader.load(fromResources("schemas"), fromResources("empty.yml"))
        }
    }

    @Test
    fun `throws exception when trying to load non-existing file`() {
        assertThrows(IllegalArgumentException::class.java) {
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
    fun `can load file without subjects`() {
        val state = loader.load(fromResources("schemas"), fromResources("no_compatibility.yml"))

        assertNull(state.compatibility)
        assertEquals(emptyList<Subject>(), state.subjects)
    }

    @Test
    fun `can load file with subjects`() {
        val schema = schemaFromResources("schemas/with_subjects.avsc")
        val state = loader.load(fromResources("schemas"), fromResources("with_subjects.yml"))

        assertNull(state.compatibility)
        assertEquals(listOf(Subject("foo", null, schema)), state.subjects)
    }

    @Test
    fun `can load file with inline schema`() {
        val schema = schemaFromResources("schemas/with_subjects.avsc")
        val state = loader.load(fromResources("schemas"), fromResources("with_inline_schema.yml"))

        assertEquals(listOf(Subject("foo", Compatibility.BACKWARD, schema)), state.subjects)
    }

    @Test
    fun `can load file with subjects and compatibility`() {
        val schema = schemaFromResources("schemas/with_subjects.avsc")
        val state = loader.load(fromResources("schemas"), fromResources("with_subjects_and_compatibility.yml"))

        assertEquals(Compatibility.FULL, state.compatibility)
        assertEquals(listOf(Subject("foo", Compatibility.FORWARD, schema)), state.subjects)
    }

    @Test
    fun `throws exception when neither schema nor file was given`() {
        assertThrows(IllegalArgumentException::class.java) {
            loader.load(fromResources("schemas"), fromResources("neither_schema_nor_file.yml"))
        }
    }

    @Test
    fun `throws exception when referenced file does not exist`() {
        assertThrows(FileNotFoundException::class.java) {
            loader.load(fromResources("schemas"), fromResources("referenced_file_does_not_exist.yml"))
        }
    }

    @Test
    fun `throws exception when subject name is missing`() {
        assertThrows(MissingKotlinParameterException::class.java) {
            loader.load(fromResources("schemas"), fromResources("subject_name_is_missing.yml"))
        }
    }

    @Test
    fun `can save state to a file`() {
        val tempFile = File.createTempFile(javaClass.simpleName, "can-save-state-to-a-file")
        tempFile.deleteOnExit()

        val outputStream = BufferedOutputStream(FileOutputStream(tempFile))

        val currentState = State(
            Compatibility.BACKWARD_TRANSITIVE,
            listOf(
                Subject("foobar-value", null, schemaFromResources("schemas/with_subjects.avsc")),
                Subject("foobar-key", Compatibility.FULL, schemaFromResources("schemas/key.avsc"))
            )
        )

        loader.save(currentState, outputStream)

        assertEquals(currentState, loader.load(fromResources("schemas"), tempFile))
    }
}
