package dev.domnikl.schema_registry_gitops.command

interface Command {
    fun execute(): Int
}
