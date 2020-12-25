package dev.domnikl.schema_registry_gitops.command

import dev.domnikl.schema_registry_gitops.*
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService
import org.apache.avro.Schema
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "apply",
    description = ["applies the config to the given schema registry"]
)
class Apply: Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var schemaRegistryGitops: SchemaRegistryGitops

    private val restService by lazy { RestService(schemaRegistryGitops.baseUrl) }
    private val client by lazy { CachedSchemaRegistryClient(restService, 100) }

    override fun call(): Int {
        val file = File("examples/schema-registry.yml")
        val state = YamlStateLoader(file.parentFile).load(file)

        if (state.compatibility != null) {
            restService.updateCompatibility(state.compatibility.toString(), "")
        }

        state.subjects.forEach { subject ->
            client.register(subject.name, subject.schema)

            if (subject.compatibility != null) {
                client.updateCompatibility(subject.name, subject.compatibility.toString())
            }
        }

        return 0
    }
}
