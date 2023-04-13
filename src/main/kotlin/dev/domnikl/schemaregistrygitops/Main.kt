package dev.domnikl.schemaregistrygitops

import dev.domnikl.schemaregistrygitops.cli.Apply
import dev.domnikl.schemaregistrygitops.cli.Dump
import dev.domnikl.schemaregistrygitops.cli.Plan
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
