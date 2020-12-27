package dev.domnikl.schema_registry_gitops.command

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.StatePersistence
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.rest.RestService
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "dump",
    description = ["prints the current state"]
)
class Dump : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var CLI: CLI

    @CommandLine.Parameters(description = ["path to output YAML file"])
    private lateinit var inputFile: String

    override fun call(): Int {
        val restService = RestService(CLI.baseUrl)
        val statePersistence = StatePersistence()

        val state = State(
            Compatibility.valueOf(restService.getConfig("").compatibilityLevel),
            restService.allSubjects.map { subject ->
                Subject(
                    subject,
                    Compatibility.valueOf(restService.getConfig(subject).compatibilityLevel),
                    AvroSchema(restService.getLatestVersion(subject).schema)
                )
            }
        )

        statePersistence.save(state, File(inputFile))

        return 0
    }
}
