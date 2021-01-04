package dev.domnikl.schema_registry_gitops.state

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.slf4j.Logger
import java.io.File
import java.io.OutputStream
import java.nio.file.Files

class Persistence(private val logger: Logger, private val schemaRegistryClient: CachedSchemaRegistryClient) {
    private val yamlFactory = YAMLFactory()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)

    private val mapper = ObjectMapper(yamlFactory)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerKotlinModule()

    fun load(basePath: File, file: File): State {
        logger.debug("Loading state file ${file.absolutePath}, referenced schemas from ${basePath.absolutePath}")

        require(Files.exists(file.toPath()))
        require(file.length() > 0)

        val yaml = mapper.readValue(file, Yaml::class.java)

        return State(
            yaml.compatibility?.let { Compatibility.valueOf(it) },
            yaml.subjects?.map {
                Subject(
                    it.name,
                    it.compatibility?.let { c -> Compatibility.valueOf(c) },
                    it.parseSchema(basePath, schemaRegistryClient),
                )
            } ?: emptyList()
        )
    }

    fun save(state: State, outputStream: OutputStream) {
        val yaml = Yaml(
            state.compatibility?.toString(),
            state.subjects.map {
                YamlSubject(
                    it.name,
                    null,
                    it.schema.canonicalString(),
                    it.compatibility?.toString(),
                    it.schema.schemaType()
                )
            }
        )

        mapper.writeValue(outputStream, yaml)
    }

    data class Yaml(val compatibility: String?, val subjects: List<YamlSubject>?)
    data class YamlSubject(val name: String, val file: String?, val schema: String?, val compatibility: String?, val type: String?) {
        fun parseSchema(basePath: File, schemaRegistryClient: CachedSchemaRegistryClient): ParsedSchema {
            val t = type ?: "AVRO"

            if (schema != null) {
                return schemaRegistryClient.parseSchema(t, schema, emptyList()).get()
            }

            if (file != null) {
                val ymlFile = File("$basePath/$file")

                return schemaRegistryClient.parseSchema(t, ymlFile.reader().readText(), emptyList()).get()
            }

            throw IllegalArgumentException("Either schema or file must be set")
        }
    }
}
