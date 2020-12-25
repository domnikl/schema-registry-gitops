package dev.domnikl.schema_registry_gitops.command

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.rest.RestService

class Dump(private val restService: RestService): Command {
    override fun execute(): Int {
        val state = State(
            Compatibility.valueOf(restService.getConfig("").compatibilityLevel),
            restService.allSubjects.map { subject ->
                Subject(
                    subject,
                    Compatibility.valueOf(restService.getConfig(subject).compatibilityLevel),
                    AvroSchema(restService.getLatestVersion(subject).schema)
                )
            }
        )

        println(state)

        return 0
    }
}
