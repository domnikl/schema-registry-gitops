package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Configuration
import dev.domnikl.schema_registry_gitops.Factory
import dev.domnikl.schema_registry_gitops.diff
import org.slf4j.Logger
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "plan",
    description = ["validate and plan schema changes, can be used to see all pending changes"],
    mixinStandardHelpOptions = true
)
class Plan(private val factory: Factory, private val logger: Logger) : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: String

    @CommandLine.Option(
        names = ["-d", "--enable-deletes"],
        description = ["allow deleting subjects not listed in input YAML"]
    )
    private var enableDeletes: Boolean = false

    override fun call(): Int {
        factory.inject(Configuration.from(cli))

        try {
            val file = File(inputFile).absoluteFile
            val state = factory.persistence.load(file.parentFile, file)
            val result = factory.diffing.diff(state, enableDeletes)

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
