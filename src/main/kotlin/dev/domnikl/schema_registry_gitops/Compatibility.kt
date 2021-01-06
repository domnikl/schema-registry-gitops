package dev.domnikl.schema_registry_gitops

enum class Compatibility {
    NONE,
    BACKWARD,
    FORWARD,
    FULL,
    BACKWARD_TRANSITIVE,
    FORWARD_TRANSITIVE,
    FULL_TRANSITIVE
}
