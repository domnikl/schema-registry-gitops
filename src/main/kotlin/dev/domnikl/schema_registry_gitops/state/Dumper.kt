package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject

class Dumper(private val client: SchemaRegistryClient) {
    fun dump() = State(
        client.globalCompatibility(),
        client.subjects().map { subject ->
            Subject(
                subject,
                client.compatibility(subject),
                client.getLatestSchema(subject)
            )
        }
    )
}
