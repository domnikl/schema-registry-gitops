package dev.domnikl.schema_registry_gitops

import picocli.CommandLine
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = exitProcess(CommandLine(CLI()).execute(*args))
