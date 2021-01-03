package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import org.slf4j.Logger

class Applier(
    private val client: SchemaRegistryClient,
    private val logger: Logger
) {
    fun apply(state: State) {
        updateGlobalCompatibility(state)

        val registeredSubjects = client.subjects()

        state.subjects.forEach { subject ->
            when (registeredSubjects.contains(subject.name)) {
                false -> register(subject)
                true -> evolve(subject)
            }
        }
    }

    private fun register(subject: Subject) {
        val versionId = client.create(subject)

        logger.info("Created subject '${subject.name}' and registered new schema with version $versionId")

        updateCompatibility(subject)
    }

    private fun evolve(subject: Subject) {
        updateCompatibility(subject)

        val versionBefore = client.version(subject)

        if (versionBefore == null) {
            val versionId = client.evolve(subject)

            logger.info("Evolved existing schema for subject '${subject.name}' to version $versionId")
        } else {
            logger.debug("Did not evolve schema, version already exists as $versionBefore")
        }
    }

    private fun updateCompatibility(subject: Subject) {
        if (subject.compatibility != null) {
            val compatibility = client.updateCompatibility(subject)

            logger.info("Changed '${subject.name}' compatibility to $compatibility")
        }
    }

    private fun updateGlobalCompatibility(state: State) {
        if (state.compatibility == null) return

        val compatibilityBefore = client.globalCompatibility()

        if (compatibilityBefore == state.compatibility) {
            logger.debug("Did not change compatibility level as it matched desired level ${state.compatibility}")
            return
        }

        val compatibilityAfter = client.updateGlobalCompatibility(state.compatibility)

        logger.info("Changed global compatibility level from $compatibilityBefore to $compatibilityAfter")
    }
}
