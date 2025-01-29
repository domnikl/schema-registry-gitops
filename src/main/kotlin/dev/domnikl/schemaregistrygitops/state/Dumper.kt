package dev.domnikl.schemaregistrygitops.state

import dev.domnikl.schemaregistrygitops.SchemaRegistryClient
import dev.domnikl.schemaregistrygitops.State
import dev.domnikl.schemaregistrygitops.Subject

class Dumper(private val client: SchemaRegistryClient) {
    fun dump() = State(
        client.globalCompatibility(),
        client.normalize(),
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
