package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.State
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService
import org.slf4j.Logger

class Applier(
    private val restService: RestService,
    private val client: SchemaRegistryClient,
    private val logger: Logger
) {
    fun apply(state: State) {
        if (state.compatibility != null) {
            val config = restService.updateCompatibility(state.compatibility.toString(), "")

            logger.info("Changed GLOBAL compatibility level to ${config.compatibilityLevel}")
        }

        state.subjects.forEach { subject ->
            val versionId = client.register(subject.name, subject.schema)

            // TODO has it really been evolved or did this version already exist?
            logger.info("Evolved schema of '${subject.name}' to version $versionId")

            if (subject.compatibility != null) {
                val compatibility = client.updateCompatibility(subject.name, subject.compatibility.toString())

                logger.info("Changed '${subject.name}' compatibility to $compatibility")
            }
        }
    }
}
