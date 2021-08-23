package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.ParsedSchema

class Diffing(private val client: SchemaRegistryClient) {
    fun diff(state: State, enableDeletes: Boolean = false): Result {
        val globalCompatibility = client.globalCompatibility()

        val compatibilityChange = if (globalCompatibility != state.compatibility && state.compatibility != null) {
            Change(globalCompatibility, state.compatibility)
        } else {
            null
        }

        val remoteSubjects = client.subjects()

        val (compatible, incompatible) = state.subjects.partition { client.testCompatibility(it) }

        val deleted = if (enableDeletes) {
            remoteSubjects.filterNot { state.subjects.map { x -> x.name }.contains(it) }
        } else {
            emptyList()
        }

        val added = compatible.filterNot { remoteSubjects.contains(it.name) }
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
        val compatibility: Change<Compatibility>? = null,
        val incompatible: List<Subject> = emptyList(),
        val added: List<Subject> = emptyList(),
        val modified: List<Changes> = emptyList(),
        val deleted: List<String> = emptyList()
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
