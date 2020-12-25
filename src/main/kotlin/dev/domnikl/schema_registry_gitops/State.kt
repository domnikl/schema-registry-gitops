package dev.domnikl.schema_registry_gitops

data class State(val compatibility: Compatibility?, val subjects: List<Subject>)
