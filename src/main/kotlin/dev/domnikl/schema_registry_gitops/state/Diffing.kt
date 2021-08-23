package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.ParsedSchema

class Diffing(private val client: SchemaRegistryClient) {
    fun diff(state: State): Result {
        val globalCompatibility = client.globalCompatibility()

        val compatibilityChange = if (globalCompatibility != state.compatibility && state.compatibility != null) {
            Change(globalCompatibility, state.compatibility)
        } else {
            null
        }

        val remoteSubjects = client.subjects()

        val (compatible, incompatible) = state.subjects.partition { client.testCompatibility(it) }

        val added = compatible.filterNot { remoteSubjects.contains(it.name) }
        val deleted = remoteSubjects.filterNot { state.subjects.map { x -> x.name }.contains(it) }
        val modified = compatible.filter { !deleted.contains(it.name) && !added.contains(it) }

        val changes = modified.mapNotNull {
            val remoteCompatibility = client.compatibility(it.name)
            val remoteSchema = client.getLatestSchema(it.name)

            val changedCompatibility = if (remoteCompatibility != it.compatibility && it.compatibility != null) {
                Change(remoteCompatibility, it.compatibility)
            } else {
                null
            }

            val changedSchema = if (!remoteSchema.deepEquals(it.schema)) {
                Change(remoteSchema, it.schema)
            } else {
                null
            }

            if (changedSchema == null && changedCompatibility == null) {
                return@mapNotNull null
            }

            Changes(it, changedCompatibility, changedSchema)
        }

        return Result(
            compatibilityChange,
            incompatible,
            added,
            changes,
            deleted
        )
    }

    data class Result(
        val compatibility: Change<Compatibility>?,
        val incompatible: List<Subject>,
        val added: List<Subject>,
        val modified: List<Changes>,
        val deleted: List<String>
    ) {
        fun isEmpty() = compatibility == null &&
            incompatible.isEmpty() &&
            added.isEmpty() &&
            modified.isEmpty() &&
            deleted.isEmpty()
    }

    data class Changes(
        val subject: Subject,
        val remoteCompatibility: Change<Compatibility>?,
        val remoteSchema: Change<ParsedSchema>?
    )

    data class Change<T>(val before: T, val after: T)
}
