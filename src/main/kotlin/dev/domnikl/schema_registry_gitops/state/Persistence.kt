package dev.domnikl.schema_registry_gitops.state

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaParseException
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference
import org.slf4j.Logger
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.util.Optional

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
                    it.references.map { yamlSubjectReference: YamlSubjectReference ->
                        SchemaReference(yamlSubjectReference.name, yamlSubjectReference.subject, yamlSubjectReference.version)
                    }
                )
            } ?: emptyList()
        )
    }

    fun save(state: State, outputStream: OutputStream) {
        val yaml = Yaml(
            (state.compatibility ?: Compatibility.NONE).toString(),
            state.subjects.map {
                YamlSubject(
                    it.name,
                    null,
                    it.schema.schemaType(),
                    it.schema.canonicalString(),
                    it.compatibility?.toString(),
                    it.references.map { subjectReference: SchemaReference ->
                        YamlSubjectReference(subjectReference.name, subjectReference.subject, subjectReference.version)
                    }
                )
            }
        )

        mapper.writeValue(outputStream, yaml)
    }

    data class Yaml(val compatibility: String?, val subjects: List<YamlSubject>?)
    data class YamlSubjectReference(val name: String, val subject: String, val version: Int)
    data class YamlSubject(val name: String, val file: String?, val type: String?, val schema: String?, val compatibility: String?, val references: List<YamlSubjectReference> = emptyList()) {
        fun parseSchema(basePath: File, schemaRegistryClient: CachedSchemaRegistryClient): ParsedSchema {
            val t = type ?: "AVRO"

            val schemaReferences = references.map {
                SchemaReference(it.name, it.subject, it.version)
            }

            val optional = when {
                file != null -> doParseSchema(schemaRegistryClient, t, File("$basePath/$file").readText(), schemaReferences)
                schema != null -> doParseSchema(schemaRegistryClient, t, schema, schemaReferences)
                else -> throw IllegalArgumentException("Either schema or file must be set")
            }

            if (optional == null || optional.isEmpty) {
                throw SchemaParseException("Could not parse $t schema for subject '$name'")
            }

            return optional.get()
        }

        private fun doParseSchema(client: CachedSchemaRegistryClient, t: String, schemaString: String, schemaReferences: List<SchemaReference>?): Optional<ParsedSchema>? {
            return client.parseSchema(t, schemaString, schemaReferences ?: emptyList())
        }
    }
}
