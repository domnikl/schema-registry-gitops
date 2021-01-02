package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient

class Validator(private val client: SchemaRegistryClient) {
    fun validate(state: State) = state.subjects.filterNot { isCompatible(it) }.map { it.name }

    private fun isCompatible(subject: Subject): Boolean {
        // if subject does not yet exist, it's always valid (as long as the schema itself is valid)
        if (!client.allSubjects.contains(subject.name)) {
            return true
        }

        return client.testCompatibility(subject.name, subject.schema)
    }
}
