package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "dump",
    description = ["prints the current state"]
)
class Dump(private val factory: Factory) : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["path to output YAML file"])
    private lateinit var inputFile: String

    override fun call(): Int {
        val state = factory.createStateDumper(cli.baseUrl).dump()

        factory.createStatePersistence().save(state, File(inputFile))

        return 0
    }
}
