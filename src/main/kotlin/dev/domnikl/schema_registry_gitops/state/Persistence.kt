package dev.domnikl.schema_registry_gitops.state

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import org.apache.avro.Schema
import java.io.File
import java.io.OutputStream
import java.nio.file.Files

class Persistence {
    private val yamlFactory = YAMLFactory()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)

    private val mapper = ObjectMapper(yamlFactory)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerKotlinModule()

    fun load(basePath: File, file: File): State {
        require(Files.exists(file.toPath()))
        require(file.length() > 0)

        val yaml = mapper.readValue(file, Yaml::class.java)

        return State(
            yaml.compatibility?.let { Compatibility.valueOf(it) },
            yaml.subjects?.map {
                Subject(
                    it.name,
                    it.compatibility?.let { c -> Compatibility.valueOf(c) },
                    AvroSchema(it.parseSchema(basePath)),
                )
            } ?: emptyList()
        )
    }

    fun save(state: State, outputStream: OutputStream) {
        val yaml = Yaml(
            state.compatibility?.toString(),
            state.subjects.map {
                val schema = it.schema.rawSchema() as Schema

                YamlSubject(
                    it.name,
                    null,
                    schema.toString(),
                    it.compatibility?.toString()
                )
            }
        )

        mapper.writeValue(outputStream, yaml)
    }

    data class Yaml(val compatibility: String?, val subjects: List<YamlSubject>?)
    data class YamlSubject(val name: String, val file: String?, val schema: String?, val compatibility: String?) {
        fun parseSchema(basePath: File): Schema {
            val parser = Schema.Parser()

            if (schema != null) {
                return parser.parse(schema)
            }

            if (file != null) {
                return parser.parse(File("$basePath/$file"))
            }

            throw IllegalArgumentException("Either schema or file must be set")
        }
    }
}
