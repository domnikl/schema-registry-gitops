package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Configuration
import dev.domnikl.schema_registry_gitops.Factory
import org.slf4j.Logger
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "plan",
    description = ["validate and plan schemas, can be used to see all pending changes"],
    mixinStandardHelpOptions = true
)
class Plan(private val factory: Factory, private val logger: Logger) : Callable<Int> {
    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["path to input YAML file"])
    private lateinit var inputFile: String

    override fun call(): Int {
        factory.inject(Configuration.from(cli))

        try {
            val file = File(inputFile).absoluteFile
            val state = factory.persistence.load(file.parentFile, file)
            val result = factory.diffing.diff(state)

            result.compatibility?.let {
                logger.info("[GLOBAL]")
                logger.info("   ~ compatibility ${it.before} -> ${it.after}")
                logger.info("")
            }

            result.deleted.forEach {
                logger.info("[SUBJECT] $it")
                logger.info("   ~ deleted")
                logger.info("")
            }

            result.added.forEach {
                logger.info("[SUBJECT] ${it.name}")
                logger.info("   ~ registered")
                logger.info("   ~ compatibility ${it.compatibility}")
                logger.info("   ~ schema ${it.schema}")
                logger.info("")
            }

            result.modified.forEach {
                logger.info("[SUBJECT] ${it.subject.name}")

                it.remoteCompatibility?.let { c ->
                    logger.info("   ~ compatibility ${c.before} -> ${c.after}")
                }

                it.remoteSchema?.let { s ->
                    logger.info("   ~ schema ${s.before} -> ${s.after}")
                }

                logger.info("")
            }

            if (result.incompatible.isNotEmpty()) {
                logger.error("[ERROR] The following schemas are incompatible with an earlier version: " +
                    "'${result.incompatible.joinToString("', '") { it.name }}'")
                return 1
            }

            if (result.isEmpty()) {
                logger.info("[SUCCESS] There are no necessary changes; the actual state matches the desired state.")
            }

            return 0
        } catch (e: Exception) {
            logger.error(e.toString())

            return 2
        }
    }
}
