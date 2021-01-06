package dev.domnikl.schema_registry_gitops

data class State(val compatibility: Compatibility?, val subjects: List<Subject>) {
    init {
        val duplicates = subjects.duplicatesBy { it.name }

        require(duplicates.isEmpty()) { "State in YAML configuration is invalid: duplicated subject(s) '${duplicates.joinToString("', '")}' found" }
    }
}

private fun <T, K> List<T>.duplicatesBy(f: (T) -> K): List<K> {
    return groupBy(f)
        .filter { it.value.size > 1 }
        .map { it.key }
}
