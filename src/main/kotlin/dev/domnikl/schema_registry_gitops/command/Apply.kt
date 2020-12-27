package dev.domnikl.schema_registry_gitops.command

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.StatePersistence
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "apply",
    description = ["applies the state to the given schema registry"]
)
class Apply : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var CLI: CLI

    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: String

    private val restService by lazy { RestService(CLI.baseUrl) }
    private val client by lazy { CachedSchemaRegistryClient(restService, 100) }

    override fun call(): Int {

        // TODO: print changes

        val file = File(inputFile)
        val state = StatePersistence().load(file.parentFile, file)

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
