package dev.domnikl.schema_registry_gitops

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val factory = Factory()
    val commandLine = CLI.commandLine(factory)

    exitProcess(commandLine.execute(*args))
}
