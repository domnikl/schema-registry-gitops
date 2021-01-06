package dev.domnikl.schema_registry_gitops.cli

import dev.domnikl.schema_registry_gitops.CLI
import dev.domnikl.schema_registry_gitops.Factory
import picocli.CommandLine
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "dump",
    description = ["prints the current state"]
)
class Dump(private val factory: Factory) : Callable<Int> {
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
        factory.baseUrl = cli.baseUrl

        val state = factory.dumper.dump()

        factory.persistence.save(state, outputStream)

        return 0
    }

    companion object {
        private const val STDOUT_FILE = "-"
    }
}
