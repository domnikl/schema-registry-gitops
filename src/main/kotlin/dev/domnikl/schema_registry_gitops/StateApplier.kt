package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService

class StateApplier(private val restService: RestService, private val client: SchemaRegistryClient) {
    fun apply(state: State) {
        if (state.compatibility != null) {
            restService.updateCompatibility(state.compatibility.toString(), "")
        }

        state.subjects.forEach { subject ->
            client.register(subject.name, subject.schema)

            if (subject.compatibility != null) {
                client.updateCompatibility(subject.name, subject.compatibility.toString())
            }
        }
    }
}
