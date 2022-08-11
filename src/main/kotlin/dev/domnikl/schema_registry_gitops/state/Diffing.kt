package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.ParsedSchema

class Diffing(private val client: SchemaRegistryClient) {
    fun diff(state: State, enableDeletes: Boolean = false): Result {
        val remoteSubjects = client.subjects()

        val (compatible, incompatible) = state.subjects.partition { client.testCompatibility(it) }

        val deleted = gatherDeletes(enableDeletes, remoteSubjects, state)
        val added = compatible.filterNot { remoteSubjects.contains(it.name) }
        val modified = compatible.filter { !deleted.contains(it.name) && !added.contains(it) }

        return Result(
            gatherCompatibilityChange(client.globalCompatibility(), state),
            incompatible,
            added,
            gatherChanges(modified),
            deleted
        )
    }

    private fun gatherCompatibilityChange(globalCompatibility: Compatibility, state: State): Change<Compatibility>? {
        if (globalCompatibility == state.compatibility || state.compatibility == null) {
            return null
        }

        return Change(globalCompatibility, state.compatibility)
    }

    private fun gatherDeletes(enableDeletes: Boolean, remoteSubjects: List<String>, state: State): List<String> {
        if (!enableDeletes) return emptyList()

        val localSubjects = state.subjects.map { it.name }

        return remoteSubjects.filterNot { localSubjects.contains(it) }
    }

    private fun gatherChanges(modified: List<Subject>) = modified.mapNotNull {
        val changedCompatibility = gatherCompatibilityChange(it)
        val changedSchema = gatherSchemaChange(it)

        if (changedSchema == null && changedCompatibility == null) {
            return@mapNotNull null
        }

        Changes(it, changedCompatibility, changedSchema)
    }

    private fun gatherCompatibilityChange(subject: Subject): Change<Compatibility>? {
        val remoteCompatibility = client.compatibility(subject.name)

        if (remoteCompatibility == subject.compatibility || subject.compatibility == null) {
            return null
        }

        return Change(remoteCompatibility, subject.compatibility)
    }

    private fun gatherSchemaChange(subject: Subject): Change<ParsedSchema>? {
        val remoteSchema = client.getLatestSchema(subject.name)

        if (remoteSchema.canonicalString() != subject.schema.canonicalString() || client.version(subject) == null) {
            return Change(remoteSchema, subject.schema)
        }

        return null
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
