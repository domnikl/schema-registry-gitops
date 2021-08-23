package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.Subject
import org.slf4j.Logger

class Applier(
    private val client: SchemaRegistryClient,
    private val logger: Logger
) {
    fun apply(diff: Diffing.Result) {
        if (diff.incompatible.isNotEmpty()) {
            // TODO: throw exception!
        }

        updateGlobalCompatibility(diff.compatibility)

        diff.deleted.forEach { delete(it) }
        diff.added.forEach { register(it) }

        diff.modified.forEach { change ->
            change.remoteCompatibility?.let {
                updateCompatibility(change.subject, it)
            }

            change.remoteSchema?.let {
                evolve(change.subject)
            }
        }
    }

    private fun delete(subject: String) {
        // TODO: delete it
    }

    private fun register(subject: Subject) {
        val versionId = client.create(subject)

        logger.info("Created subject '${subject.name}' and registered new schema with version $versionId")

        subject.compatibility?.let {
            updateCompatibility(subject, Diffing.Change(Compatibility.NONE, subject.compatibility))
        }
    }

    private fun evolve(subject: Subject) {
        val versionBefore = client.version(subject)

        if (versionBefore == null) {
            val versionId = client.evolve(subject)

            logger.info("Evolved existing schema for subject '${subject.name}' to version $versionId")
        } else {
            logger.debug("Did not evolve schema, version already exists as $versionBefore")
        }
    }

    private fun updateCompatibility(subject: Subject, change: Diffing.Change<Compatibility>?) {
        if (change == null) {
            logger.debug("Did not change compatibility level for '${subject.name}' as it matched desired level ${subject.compatibility}")
            return
        }

        val compatibility = client.updateCompatibility(subject)

        logger.info("Changed '${subject.name}' compatibility from ${change.before} to $compatibility")
    }

    private fun updateGlobalCompatibility(change: Diffing.Change<Compatibility>?) {
        if (change == null) return

        val compatibilityAfter = client.updateGlobalCompatibility(change.after)

        logger.info("Changed global compatibility level from ${change.before} to $compatibilityAfter")
    }
}
