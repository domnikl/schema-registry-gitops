package dev.domnikl.schema_registry_gitops

data class State(val compatibility: Compatibility?, val subjects: List<Subject>) {
    init {
        val duplicates = subjects.findDuplicates()

        require(duplicates.isEmpty()) { "State in YAML configuration is invalid: duplicated subject(s) '${duplicates.joinToString("', '")}' found" }
    }
}

private fun List<Subject>.findDuplicates(): List<String> {
    return this.groupBy { it.name }.filter { it.value.size > 1 }.map { it.key }
}
