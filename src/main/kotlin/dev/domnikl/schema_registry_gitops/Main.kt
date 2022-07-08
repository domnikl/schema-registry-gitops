package dev.domnikl.schema_registry_gitops

import dev.domnikl.schema_registry_gitops.cli.Apply
import dev.domnikl.schema_registry_gitops.cli.Dump
import dev.domnikl.schema_registry_gitops.cli.Plan
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.Banner
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import picocli.CommandLine
import picocli.CommandLine.IFactory
import kotlin.system.exitProcess

@SpringBootApplication
class SchemaRegistryGitopsApplication(private val factory: IFactory, private val cli: CLI) : CommandLineRunner, ExitCodeGenerator {
    private var exitCode: Int? = null

    override fun run(vararg args: String?) {
        exitCode = CommandLine(cli, factory)
            .addSubcommand("apply", Apply::class.java)
            .addSubcommand("dump", Dump::class.java)
            .addSubcommand("plan", Plan::class.java)
            .execute(*args)
    }

    override fun getExitCode() = exitCode ?: 1

    @Bean
    fun configuration(): Configuration {
        return Configuration.from(cli, System.getenv())
    }

    @Bean
    fun cachedSchemaRegistryClient(configuration: Configuration) = CachedSchemaRegistryClient(
        RestService(configuration.baseUrl),
        100,
        listOf(AvroSchemaProvider(), ProtobufSchemaProvider(), JsonSchemaProvider()),
        configuration.toMap(),
        null
    )

    @Bean
    fun logger(): Logger = LoggerFactory.getLogger(SchemaRegistryGitopsApplication::class.java)
}

fun main(args: Array<String>) {
    val app = SpringApplicationBuilder(SchemaRegistryGitopsApplication::class.java)
        .bannerMode(Banner.Mode.OFF)
        .lazyInitialization(true)

    exitProcess(SpringApplication.exit(app.run(*args)))
}
