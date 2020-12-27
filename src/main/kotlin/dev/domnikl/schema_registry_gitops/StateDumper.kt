package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.rest.RestService
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException

class StateDumper(private val restService: RestService) {
    fun dump() = State(
        Compatibility.valueOf(restService.getConfig("").compatibilityLevel),
        restService.allSubjects.map { subject ->
            val compatibility = try {
                Compatibility.valueOf(restService.getConfig(subject).compatibilityLevel)
            } catch (e: RestClientException) {
                Compatibility.NONE
            }

            Subject(
                subject,
                compatibility,
                AvroSchema(restService.getLatestVersion(subject).schema)
            )
        }
    )
}
