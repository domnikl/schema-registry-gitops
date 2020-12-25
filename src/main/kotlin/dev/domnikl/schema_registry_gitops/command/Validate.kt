package dev.domnikl.schema_registry_gitops.command

import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient

class Validate(private val client: SchemaRegistryClient, private val state: State): Command {
    override fun execute(): Int {
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
