package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import org.springframework.stereotype.Component

@Component
class Dumper(private val client: SchemaRegistryClient) {
    fun dump() = State(
        client.globalCompatibility(),
        client.subjects().map { subject ->
            val schema = client.getLatestSchema(subject)
            Subject(
                subject,
                client.compatibility(subject),
                schema,
                schema.references()
            )
        }
    )
}
