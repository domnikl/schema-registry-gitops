package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient

class StateValidator(private val client: SchemaRegistryClient) {
    fun validate(state: State) = state.subjects.filterNot { isCompatible(it) }.map { it.name }

    private fun isCompatible(subject: Subject): Boolean {
        // if subject does not yet exist, it's always valid (as long as the schema itself is valid)
        if (!client.allSubjects.contains(subject.name)) {
            return true
        }

        return client.testCompatibility(subject.name, subject.schema)
    }
}
