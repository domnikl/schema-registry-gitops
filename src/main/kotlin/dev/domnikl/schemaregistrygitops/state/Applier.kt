package dev.domnikl.schemaregistrygitops.state

import dev.domnikl.schemaregistrygitops.Compatibility
import dev.domnikl.schemaregistrygitops.SchemaRegistryClient
import dev.domnikl.schemaregistrygitops.Subject
import dev.domnikl.schemaregistrygitops.diff
import io.confluent.kafka.schemaregistry.ParsedSchema
import org.slf4j.Logger

enum class Result {
    ERROR,
    SUCCESS
}

class Applier(
    private val client: SchemaRegistryClient,
    private val logger: Logger
) {
    fun apply(diff: Diffing.Result): Result {
        if (diff.incompatible.isNotEmpty()) {
            diff.incompatible.forEach {
                logger.error(
                    "[ERROR] The following schema is incompatible with an earlier version: '${ it.subject.name }': '" +
                        it.messages.joinToString(",") +
                        "'"
                )
            }

            return Result.ERROR
        }

        diff.compatibility?.let {
            updateGlobalCompatibility(diff.compatibility)

            logger.info("[GLOBAL]")
            logger.info("   ~ compatibility ${diff.compatibility.before} -> ${diff.compatibility.after}")
            logger.info("")
        }

        diff.normalize?.let {
            updateNormalize(diff.normalize)
            logger.info("[GLOBAL]")
            logger.info("   ~ normalize ${diff.normalize.before} -> ${diff.normalize.after}")
            logger.info("")
        }

        diff.deleted.forEach { delete(it) }
        diff.added.forEach { register(it) }

        diff.modified.forEach { change ->
            logger.info("[SUBJECT] ${change.subject.name}")

            change.remoteCompatibility?.let {
                updateCompatibility(change.subject)
                logger.info("   ~ compatibility ${change.remoteCompatibility.before} -> ${change.remoteCompatibility.after}")
            }

            change.remoteSchema?.let {
                evolve(change.subject, it)
            }
        }

        return Result.SUCCESS
    }

    private fun delete(subject: String) {
        client.delete(subject)

        logger.info("[SUBJECT] $subject")
        logger.info("   - deleted")
        logger.info("")
    }

    private fun register(subject: Subject) {
        val versionId = client.create(subject)

        logger.info("[SUBJECT] ${subject.name}")
        logger.info("   + registered (version $versionId)")

        subject.compatibility?.let {
            updateCompatibility(subject)
            logger.info("   + compatibility ${subject.compatibility}")
        }

        logger.info("")
    }

    private fun evolve(subject: Subject, change: Diffing.Change<ParsedSchema>) {
        val versionId = client.evolve(subject)

        logger.info("   ~ evolved (version $versionId)")
        logger.info("   ~ schema ${change.before.diff(change.after)}")
        logger.info("")
    }

    private fun updateCompatibility(subject: Subject) {
        client.updateCompatibility(subject)
    }

    private fun updateGlobalCompatibility(change: Diffing.Change<Compatibility>) {
        client.updateGlobalCompatibility(change.after)
    }

    private fun updateNormalize(change: Diffing.Change<Boolean>) {
        client.updateNormalize(change.after)
    }
}
