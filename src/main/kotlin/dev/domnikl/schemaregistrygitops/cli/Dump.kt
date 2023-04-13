package dev.domnikl.schemaregistrygitops.cli

import dev.domnikl.schemaregistrygitops.CLI
import dev.domnikl.schemaregistrygitops.Configuration
import dev.domnikl.schemaregistrygitops.state.Dumper
import dev.domnikl.schemaregistrygitops.state.Persistence
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "dump",
    description = ["prints the current state"]
)
class Dump(
    private val configuration: Configuration? = null,
    private val persistence: Persistence? = null,
    private val dumper: Dumper? = null
) : Callable<Int> {
    private val logger = LoggerFactory.getLogger(Dump::class.java)

    @CommandLine.ParentCommand
    private lateinit var cli: CLI

    @CommandLine.Parameters(description = ["optional path to output YAML file, default is \"-\", which prints to STDOUT"], defaultValue = STDOUT_FILE)
    private lateinit var outputFile: String

    private val outputStream by lazy {
        when (outputFile) {
            STDOUT_FILE -> System.out
            else -> BufferedOutputStream(FileOutputStream(File(outputFile)))
        }
    }

    override fun call(): Int {
        val configuration = configuration ?: Configuration.from(cli)
        val persistence = persistence ?: Persistence(configuration.client(), logger)
        val dumper = dumper ?: Dumper(configuration.schemaRegistryClient())
        val state = dumper.dump()

        persistence.save(state, outputStream)

        return 0
    }

    companion object {
        private const val STDOUT_FILE = "-"
    }
}
