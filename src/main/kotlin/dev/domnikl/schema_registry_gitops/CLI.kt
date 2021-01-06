package dev.domnikl.schema_registry_gitops

import dev.domnikl.schema_registry_gitops.cli.Apply
import dev.domnikl.schema_registry_gitops.cli.Dump
import dev.domnikl.schema_registry_gitops.cli.Validate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.InputStreamReader
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import ch.qos.logback.classic.Logger as LogbackClassicLogger

@CommandLine.Command(
    name = "schema-registry-gitops",
    mixinStandardHelpOptions = true,
    versionProvider = CLI.Companion::class,
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
            val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as LogbackClassicLogger
            rootLogger.level = ch.qos.logback.classic.Level.DEBUG
        }
    }

    override fun call(): Int {
        CommandLine.usage(this, System.out)
        exitProcess(0)
    }

    companion object : CommandLine.IVersionProvider {
        fun commandLine(factory: Factory, logger: Logger = LoggerFactory.getLogger(CLI::class.java)): CommandLine {
            return CommandLine(CLI())
                .addSubcommand(Validate(factory, logger))
                .addSubcommand(Apply(factory, logger))
                .addSubcommand(Dump(factory))
        }

        override fun getVersion(): Array<String> {
            val version = InputStreamReader(object {}::class.java.classLoader.getResourceAsStream("version.txt")!!).readText()

            return arrayOf("schema-registry-gitops $version")
        }
    }
}
