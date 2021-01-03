package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import org.slf4j.Logger

class Applier(
    private val client: SchemaRegistryClient,
    private val logger: Logger
) {
    fun apply(state: State) {
        updateDefaultCompatibility(state)

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

        logger.info("Created subject '${subject.name}' and registered new schema with version $versionId")

        updateCompatibility(subject)
    }

    private fun evolve(subject: Subject) {
        updateCompatibility(subject)

        val versionBefore = try {
            client.getVersion(subject.name, subject.schema)
        } catch (e: RestClientException) {
            when (e.errorCode) {
                40403 -> null
                else -> throw e
            }
        }

        if (versionBefore == null) {
            val versionId = client.register(subject.name, subject.schema)

            logger.info("Evolved existing schema for subject '${subject.name}' to version $versionId")
        } else {
            logger.debug("Did not evolve schema, version already exists as $versionBefore")
        }
    }

    private fun updateCompatibility(subject: Subject) {
        if (subject.compatibility != null) {
            val compatibility = client.updateCompatibility(subject.name, subject.compatibility.toString())

            logger.info("Changed '${subject.name}' compatibility to $compatibility")
        }
    }

    private fun updateDefaultCompatibility(state: State) {
        if (state.compatibility == null) return

        val compatibilityBefore = Compatibility.valueOf(client.getCompatibility(""))

        if (compatibilityBefore == state.compatibility) {
            logger.debug("Did not change compatibility level as it matched desired level ${state.compatibility}")
            return
        }

        val compatibilityAfter = client.updateCompatibility("", state.compatibility.toString())

        logger.info("Changed default compatibility level from $compatibilityBefore to $compatibilityAfter")
    }
}
