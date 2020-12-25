package dev.domnikl.schema_registry_gitops

import dev.domnikl.schema_registry_gitops.command.Apply
import dev.domnikl.schema_registry_gitops.command.Dump
import dev.domnikl.schema_registry_gitops.command.Validate
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess


@CommandLine.Command(
    name = "schema-registry-gitops",
    mixinStandardHelpOptions = true,
    version = ["schema-registry-gitops 0.1"],
    description = ["Manages schema registries through Infrastructure as Code"],
    subcommands = [
        Validate::class,
        Apply::class,
        Dump::class,
    ]
)
class SchemaRegistryGitops: Callable<Int> {
    @CommandLine.Option(names = ["-r", "--registry"], scope = CommandLine.ScopeType.INHERIT)
    var baseUrl = "http://localhost:8081"

    override fun call(): Int {
        CommandLine.usage(this, System.out)
        exitProcess(0)
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(SchemaRegistryGitops()).execute(*args))
