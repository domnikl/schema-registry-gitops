package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "apply",
    description = ["applies the state to the given schema registry"]
)
class Apply(private val factory: Factory) : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: String

    override fun call(): Int {
        val file = File(inputFile)
        val state = factory.createStatePersistence().load(file.parentFile, file)
        val stateApplier = factory.createStateApplier(cli.baseUrl)

        stateApplier.apply(state)

        return 0
    }
}
