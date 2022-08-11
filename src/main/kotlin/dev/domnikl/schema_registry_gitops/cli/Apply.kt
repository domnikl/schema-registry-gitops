package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Configuration
import dev.domnikl.schema_registry_gitops.state.Applier
import dev.domnikl.schema_registry_gitops.state.Diffing
import dev.domnikl.schema_registry_gitops.state.Persistence
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "apply",
    description = ["applies the state to the given schema registry"]
)
class Apply(
    private val configuration: Configuration? = null,
    private val persistence: Persistence? = null,
    private val diffing: Diffing? = null,
    private val applier: Applier? = null,
    logger: Logger? = null
) : Callable<Int> {
    private val logger = logger ?: LoggerFactory.getLogger(Apply::class.java)

    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: File

    @CommandLine.Option(
        names = ["-d", "--enable-deletes"],
        description = ["allow deleting subjects not listed in input YAML"]
    )
    private var enableDeletes: Boolean = false

    override fun call(): Int {
        return try {
            val configuration = configuration ?: Configuration.from(cli)
            val persistence = persistence ?: Persistence(configuration.client(), logger)
            val diffing = diffing ?: Diffing(configuration.schemaRegistryClient())
            val applier = applier ?: Applier(configuration.schemaRegistryClient(), logger)

            val state = persistence.load(inputFile.absoluteFile.parentFile, inputFile.absoluteFile)
            val diff = diffing.diff(state, enableDeletes)

            applier.apply(diff)

            logger.info("[SUCCESS] Applied state from $inputFile to ${configuration.baseUrl}")

            0
        } catch (e: Exception) {
            logger.error(e.toString())

            1
        }
    }
}
