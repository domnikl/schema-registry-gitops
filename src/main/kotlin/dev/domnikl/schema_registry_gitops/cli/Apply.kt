package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Configuration
import dev.domnikl.schema_registry_gitops.state.Applier
import dev.domnikl.schema_registry_gitops.state.Diffing
import dev.domnikl.schema_registry_gitops.state.Persistence
import org.slf4j.Logger
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject

@CommandLine.Command(
    name = "apply",
    description = ["applies the state to the given schema registry"]
)
class Apply @Inject constructor(
    private val configuration: Configuration,
    private val persistence: Persistence,
    private val diffing: Diffing,
    private val applier: Applier,
    private val logger: Logger
) : Callable<Int> {
    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: String

    @CommandLine.Option(
        names = ["-d", "--enable-deletes"],
        description = ["allow deleting subjects not listed in input YAML"]
    )
    private var enableDeletes: Boolean = false

    override fun call(): Int {
        return try {
            val file = File(inputFile).absoluteFile
            val state = persistence.load(file.parentFile, file)
            val diff = diffing.diff(state, enableDeletes)

            applier.apply(diff)

            logger.info("[SUCCESS] Applied state from $file to ${configuration.baseUrl}")

            0
        } catch (e: Exception) {
            logger.error(e.toString())

            1
        }
    }
}
