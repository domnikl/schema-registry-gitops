package dev.domnikl.schemaregistrygitops

enum class Compatibility {
    NONE,
    BACKWARD,
    FORWARD,
    FULL,
    BACKWARD_TRANSITIVE,
    FORWARD_TRANSITIVE,
    FULL_TRANSITIVE
}
