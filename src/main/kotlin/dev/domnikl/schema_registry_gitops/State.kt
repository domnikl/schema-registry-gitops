package dev.domnikl.schema_registry_gitops

data class State(val compatibility: Compatibility?, val subjects: List<Subject>) {
    init {
        val duplicates = subjects.duplicatesBy { it.name }

        require(duplicates.isEmpty()) { "State in YAML configuration is invalid: duplicated subject(s) '${duplicates.joinToString("', '")}' found" }
    }

    fun merge(other: State): State {
        val a = subjects.associateBy { it.name }
        val b = other.subjects.associateBy { it.name }

        return State(
            compatibility ?: other.compatibility,
            (a + b).map { it.value }
        )
    }
}

private fun <T, K> List<T>.duplicatesBy(f: (T) -> K): List<K> {
    return groupBy(f)
        .filter { it.value.size > 1 }
        .map { it.key }
}
