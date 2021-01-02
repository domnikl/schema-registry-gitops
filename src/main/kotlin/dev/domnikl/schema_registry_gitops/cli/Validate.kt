package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import dev.domnikl.schema_registry_gitops.state.Persistence
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "validate",
    description = ["validate schemas, should be used before applying changes"]
)
class Validate(factory: Factory) : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: String

    private val validator by lazy { factory.createStateValidator(cli.baseUrl) }

    override fun call(): Int {
        val file = File(inputFile)
        val state = Persistence().load(file.parentFile, file)
        val incompatibleSchemas = validator.validate(state)

        if (incompatibleSchemas.isEmpty()) {
            return 0
        }

        println("The following schemas are incompatible with an earlier version:")
        println("")

        incompatibleSchemas.forEach {
            println("  - $it")
        }

        println("")
        println("VALIDATION FAILED")

        return 1
    }
}
