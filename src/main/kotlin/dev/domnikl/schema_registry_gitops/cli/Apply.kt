package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Configuration
import dev.domnikl.schema_registry_gitops.Factory
import org.slf4j.Logger
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "apply",
    description = ["applies the state to the given schema registry"]
)
class Apply(private val factory: Factory, private val logger: Logger) : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: String

    override fun call(): Int {
        val configuration = Configuration.from(cli)

        factory.inject(configuration)

        return try {
            val file = File(inputFile).absoluteFile
            val state = factory.persistence.load(file.parentFile, file)
            val diff = factory.diffing.diff(state)

            factory.applier.apply(diff)

            logger.info("[SUCCESS] Applied state from $file to ${configuration.baseUrl}")

            0
        } catch (e: Exception) {
            logger.error(e.toString())

            1
        }
    }
}
