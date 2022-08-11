package dev.domnikl.schema_registry_gitops

import dev.domnikl.schema_registry_gitops.cli.Apply
import dev.domnikl.schema_registry_gitops.cli.Dump
import dev.domnikl.schema_registry_gitops.cli.Plan
import picocli.CommandLine
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = CommandLine(CLI())
        .addSubcommand("apply", Apply::class.java)
        .addSubcommand("dump", Dump::class.java)
        .addSubcommand("plan", Plan::class.java)
        .execute(*args)

    exitProcess(exitCode)
}
