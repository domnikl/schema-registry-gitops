package dev.domnikl.schema_registry_gitops.command

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

import dev.domnikl.schema_registry_gitops.*


@CommandLine.Command(
    name = "validate",
    description = ["validate schemas, should be used before applying changes"]
)
class Validate: Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var schemaRegistryGitops: SchemaRegistryGitops

    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: String

    private val restService by lazy { RestService(schemaRegistryGitops.baseUrl) }
    private val client by lazy { CachedSchemaRegistryClient(restService, 100) }

    override fun call(): Int {
        val file = File(inputFile)
        val state = StatePersistence().load(file.parentFile, file)
        val incompatibleSchemas = state.subjects.filterNot { isCompatible(it) }.map { it.name }

        if (incompatibleSchemas.isEmpty()) {
            return 0
        }

        println("The following schemas are incompatible with an earlier schema:")
        println("")

        incompatibleSchemas.forEach {
            println("  - $it")
        }

        println("")
        println("VALIDATION FAILED")

        return 1
    }

    private fun isCompatible(subject: Subject): Boolean {
        // if subject does not yet exist, it's always valid (as long as the schema itself is valid)
        if (!client.allSubjects.contains(subject.name)) {
            return true
        }

        return client.testCompatibility(subject.name, subject.schema)
    }
}
