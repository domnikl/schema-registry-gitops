package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException

class Dumper(private val client: SchemaRegistryClient) {
    fun dump() = State(
        Compatibility.valueOf(client.getCompatibility("")),
        client.allSubjects.map { subject ->
            val compatibility = try {
                Compatibility.valueOf(client.getCompatibility(subject))
            } catch (e: RestClientException) {
                Compatibility.NONE
            }

            Subject(
                subject,
                compatibility,
                AvroSchema(client.getLatestSchemaMetadata(subject).schema)
            )
        }
    )
}
