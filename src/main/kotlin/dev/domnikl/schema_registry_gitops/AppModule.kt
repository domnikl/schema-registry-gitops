package dev.domnikl.schema_registry_gitops

import dagger.Module
import dagger.Provides
import dev.domnikl.schema_registry_gitops.cli.Apply
import dev.domnikl.schema_registry_gitops.cli.Dump
import dev.domnikl.schema_registry_gitops.cli.Plan
import dev.domnikl.schema_registry_gitops.state.Applier
import dev.domnikl.schema_registry_gitops.state.Diffing
import dev.domnikl.schema_registry_gitops.state.Dumper
import dev.domnikl.schema_registry_gitops.state.Persistence
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine

@Module
class AppModule {
    @Provides
    fun createCli() = CLI()

    @Provides
    fun createApply(
        configuration: Configuration,
        persistence: Persistence,
        diffing: Diffing,
        applier: Applier,
        logger: Logger
    ) = Apply(
        configuration,
        persistence,
        diffing,
        applier,
        logger
    )

    @Provides
    fun createPlan(
        persistence: Persistence,
        diffing: Diffing,
        logger: Logger
    ) = Plan(
        persistence,
        diffing,
        logger
    )

    @Provides
    fun createDump(
        persistence: Persistence,
        dumper: Dumper
    ) = Dump(
        persistence,
        dumper
    )

    @Provides
    fun commandLine(cli: CLI): CommandLine {
        return CommandLine(cli)
            .addSubcommand(Plan::class.java)
            .addSubcommand(Apply::class.java)
            .addSubcommand(Dump::class.java)
    }

    @Provides
    fun createConfiguration(cli: CLI): Configuration {
        return Configuration.from(cli, System.getenv())
    }

    @Provides
    fun createApplier(client: SchemaRegistryClient, logger: Logger) = Applier(client, logger)

    @Provides
    fun createDumper(client: SchemaRegistryClient) = Dumper(client)

    @Provides
    fun createDiffing(client: SchemaRegistryClient) = Diffing(client)

    @Provides
    fun createPersistence(client: CachedSchemaRegistryClient, logger: Logger) = Persistence(client, logger)

    @Provides
    fun createLogger(): Logger = LoggerFactory.getLogger(AppModule::class.java)

    @Provides
    fun createCachedClient(configuration: Configuration) = CachedSchemaRegistryClient(
        RestService(configuration.baseUrl),
        100,
        listOf(AvroSchemaProvider(), ProtobufSchemaProvider(), JsonSchemaProvider()),
        configuration.toMap(),
        null
    )

    @Provides
    fun createClient(client: CachedSchemaRegistryClient) = SchemaRegistryClient(client)
}
