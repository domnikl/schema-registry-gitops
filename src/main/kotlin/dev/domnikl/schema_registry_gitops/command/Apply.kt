package dev.domnikl.schema_registry_gitops.command

import dev.domnikl.schema_registry_gitops.State
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService

class Apply(private val restService: RestService, private val client: SchemaRegistryClient, private val state: State): Command {
    override fun execute(): Int {
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
