package dev.domnikl.schema_registry_gitops

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import org.apache.avro.Schema
import java.io.File
import java.nio.file.Files

class StatePersistence {
    private val mapper = ObjectMapper(YAMLFactory())
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

    fun save(state: State, toFile: File) {
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

        mapper.writeValue(toFile, yaml)
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
