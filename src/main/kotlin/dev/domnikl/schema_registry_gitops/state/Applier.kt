package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.slf4j.Logger

class Applier(
    private val client: SchemaRegistryClient,
    private val logger: Logger
) {
    fun apply(state: State) {
        if (state.compatibility != null) {
            val compatibility = client.updateCompatibility("", state.compatibility.toString())

            logger.info("Changed default compatibility level to $compatibility")
        }

        val registeredSubjects = client.allSubjects

        state.subjects.forEach { subject ->
            when (registeredSubjects.contains(subject.name)) {
                false -> register(subject)
                true -> evolve(subject)
            }
        }
    }

    private fun register(subject: Subject) {
        val versionId = client.register(subject.name, subject.schema)

        logger.info("Registered new schema for '${subject.name}' with version $versionId")

        updateCompatibility(subject)
    }

    private fun evolve(subject: Subject) {
        updateCompatibility(subject)

        val versionId = client.register(subject.name, subject.schema)

        logger.info("Evolved existing schema for '${subject.name}' to version $versionId")
    }

    private fun updateCompatibility(subject: Subject) {
        if (subject.compatibility != null) {
            val compatibility = client.updateCompatibility(subject.name, subject.compatibility.toString())

            logger.info("Changed '${subject.name}' compatibility to $compatibility")
        }
    }
}
