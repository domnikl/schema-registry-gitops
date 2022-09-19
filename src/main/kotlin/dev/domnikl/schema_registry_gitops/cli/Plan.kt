package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Configuration
import dev.domnikl.schema_registry_gitops.diff
import dev.domnikl.schema_registry_gitops.state.Diffing
import dev.domnikl.schema_registry_gitops.state.Persistence
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import picocli.CommandLine.ParentCommand
import java.io.File
import java.util.concurrent.Callable

@Command(
    name = "plan",
    description = ["validate and plan schema changes, can be used to see all pending changes"],
    mixinStandardHelpOptions = true
)
class Plan(
    private val configuration: Configuration? = null,
    private val persistence: Persistence? = null,
    private val diffing: Diffing? = null,
    logger: Logger? = null
) : Callable<Int> {
    private val logger = logger ?: LoggerFactory.getLogger(Plan::class.java)

    @ParentCommand
    private lateinit var cli: CLI

    @Parameters(description = ["path to input YAML files"])
    private lateinit var inputFiles: List<File>

    @Option(
        names = ["-d", "--enable-deletes"],
        description = ["allow deleting subjects not listed in input YAML"]
    )
    private var enableDeletes: Boolean = false

    override fun call(): Int {
        try {
            val configuration = configuration ?: Configuration.from(cli)
            val persistence = persistence ?: Persistence(configuration.client(), logger)
            val diffing = diffing ?: Diffing(configuration.schemaRegistryClient())

            val state = persistence.load(inputFiles.first().absoluteFile.parentFile, inputFiles.map { it.absoluteFile })
            val result = diffing.diff(state, enableDeletes)

            if (!result.isEmpty()) {
                logger.info("The following changes would be applied:")
                logger.info("")
            }

            result.compatibility?.let {
                logger.info("[GLOBAL]")
                logger.info("   ~ compatibility ${it.before} -> ${it.after}")
                logger.info("")
            }

            if (enableDeletes) {
                result.deleted.forEach {
                    logger.info("[SUBJECT] $it")
                    logger.info("   - delete")
                    logger.info("")
                }
            }

            result.added.forEach {
                logger.info("[SUBJECT] ${it.name}")
                logger.info("   + register")

                it.compatibility?.let { c ->
                    logger.info("   + compatibility $c")
                }

                logger.info("   + schema ${it.schema}")
                logger.info("")
            }

            result.modified.forEach {
                logger.info("[SUBJECT] ${it.subject.name}")

                it.remoteCompatibility?.let { c ->
                    logger.info("   ~ compatibility ${c.before} -> ${c.after}")
                }

                it.remoteSchema?.let { s ->
                    logger.info("   ~ schema ${s.before.diff(s.after)}")
                }

                logger.info("")
            }

            if (result.incompatible.isNotEmpty()) {
                logger.error(
                    "[ERROR] The following schemas are incompatible with an earlier version: " +
                        "'${result.incompatible.joinToString("', '") { it.name }}'"
                )

                return 1
            }

            if (result.isEmpty()) {
                logger.info("[SUCCESS] There are no necessary changes; the actual state matches the desired state.")
            } else {
                logger.info("[SUCCESS] All changes are compatible and can be applied.")
            }

            return 0
        } catch (e: Exception) {
            logger.error(e.toString())

            return 2
        }
    }
}
