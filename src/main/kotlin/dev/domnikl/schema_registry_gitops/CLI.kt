package dev.domnikl.schema_registry_gitops

import ch.qos.logback.classic.Logger
import dev.domnikl.schema_registry_gitops.cli.Apply
import dev.domnikl.schema_registry_gitops.cli.Dump
import dev.domnikl.schema_registry_gitops.cli.Validate
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "schema-registry-gitops",
    mixinStandardHelpOptions = true,
    version = ["schema-registry-gitops 0.1"],
    description = ["Manages schema registries through Infrastructure as Code"]
)
class CLI : Callable<Int> {
    @CommandLine.Option(
        names = ["-r", "--registry"],
        description = ["schema registry HTTP endpoint"],
        scope = CommandLine.ScopeType.INHERIT
    )
    var baseUrl = "http://localhost:8081"

    @CommandLine.Option(
        names = ["-v", "--verbose"],
        description = ["enable verbose logging"],
        scope = CommandLine.ScopeType.INHERIT
    )
    fun setVerbose(verbose: Boolean) {
        if (verbose) {
            val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
            rootLogger.level = ch.qos.logback.classic.Level.DEBUG
        }
    }

    override fun call(): Int {
        CommandLine.usage(this, System.out)
        exitProcess(0)
    }

    companion object {
        fun commandLine(factory: Factory, logger: Logger = LoggerFactory.getLogger(Validate::class.java) as Logger): CommandLine {
            return CommandLine(CLI())
                .addSubcommand(Validate(factory, logger))
                .addSubcommand(Apply(factory))
                .addSubcommand(Dump(factory))
        }
    }
}
