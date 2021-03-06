package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Configuration
import dev.domnikl.schema_registry_gitops.Factory
import org.slf4j.Logger
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "validate",
    description = ["validate schemas, should be used before applying changes"],
    mixinStandardHelpOptions = true
)
class Validate(private val factory: Factory, private val logger: Logger) : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: String

    override fun call(): Int {
        factory.inject(Configuration.from(cli))

        try {
            val file = File(inputFile).absoluteFile
            val state = factory.persistence.load(file.parentFile, file)
            val incompatibleSubjects = factory.validator.validate(state)

            state.subjects.forEach { subject ->
                if (incompatibleSubjects.contains(subject.name)) {
                    logger.error("Subject '${subject.name}': FAIL")
                } else {
                    logger.info("Subject '${subject.name}': ok")
                }
            }

            if (incompatibleSubjects.isEmpty()) {
                logger.info("VALIDATION PASSED: all schemas are ready to be evolved")
                return 0
            }

            logger.error(
                "VALIDATION FAILED: The following schemas are incompatible with an earlier version: " +
                    "'${incompatibleSubjects.joinToString("', '")}'"
            )

            return 1
        } catch (e: Exception) {
            logger.error(e.toString())

            return 2
        }
    }
}
