package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import org.slf4j.Logger
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "validate",
    description = ["validate schemas, should be used before applying changes"]
)
class Validate(private val factory: Factory, private val logger: Logger) : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: String

    override fun call(): Int {
        factory.baseUrl = cli.baseUrl

        try {
            val file = File(inputFile)
            val state = factory.persistence.load(file.parentFile, file)
            val incompatibleSchemas = factory.validator.validate(state)

            if (incompatibleSchemas.isEmpty()) {
                logger.debug("VALIDATION PASSED: all schemas are ready to be evolved")
                return 0
            }

            logger.error(
                "VALIDATION FAILED: The following schemas are incompatible with an earlier version: " +
                    "'${incompatibleSchemas.joinToString("', '")}'"
            )

            return 1
        } catch (e: Exception) {
            logger.error(e.toString())

            return 2
        }
    }
}
