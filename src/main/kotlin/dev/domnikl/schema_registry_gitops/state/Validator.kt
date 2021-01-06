package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject

class Validator(private val client: SchemaRegistryClient) {
    fun validate(state: State) = state.subjects.filterNot { isCompatible(it) }.map { it.name }

    private fun isCompatible(subject: Subject): Boolean {
        // if subject does not yet exist, it's always valid (as long as the schema itself is valid)
        if (!client.subjects().contains(subject.name)) {
            return true
        }

        return client.testCompatibility(subject)
    }
}
