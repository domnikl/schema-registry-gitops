package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import picocli.CommandLine
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "dump",
    description = ["prints the current state"]
)
class Dump(private val factory: Factory) : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["path to output YAML file"])
    private lateinit var outputFile: String

    override fun call(): Int {
        val state = factory.createStateDumper(cli.baseUrl).dump()
        val outputStream = BufferedOutputStream(FileOutputStream(File(outputFile)))

        factory.createStatePersistence().save(state, outputStream)

        return 0
    }
}
