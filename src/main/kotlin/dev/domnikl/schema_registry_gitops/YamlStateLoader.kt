package dev.domnikl.schema_registry_gitops

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import org.apache.avro.Schema
import java.io.File

class YamlStateLoader {
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    private val parser = Schema.Parser()

    fun load(file: File): State {
        val yaml = mapper.readValue(file, Yaml::class.java)
        val basePath = file.parentFile

        return State(
            yaml.compatibility?.let { Compatibility.valueOf(it) },
            yaml.subjects.map {
                Subject(
                    it.name,
                    it.compatibility?.let { c -> Compatibility.valueOf(c) },
                    AvroSchema(parser.parse(File("$basePath/${it.file}"))),
                )
            }
        )
    }

    data class Yaml(val compatibility: String?, val subjects: List<YamlSubject>)
    data class YamlSubject(val name: String, val file: String, val compatibility: String?)
}
