package dev.domnikl.schema_registry_gitops

import dev.domnikl.schema_registry_gitops.state.Applier
import dev.domnikl.schema_registry_gitops.state.Diffing
import dev.domnikl.schema_registry_gitops.state.Dumper
import dev.domnikl.schema_registry_gitops.state.Persistence
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider
import org.slf4j.LoggerFactory

class Factory {
    private lateinit var config: Configuration

    val diffing by lazy { Diffing(client) }
    val applier by lazy { Applier(client, LoggerFactory.getLogger(Applier::class.java)) }
    val dumper by lazy { Dumper(client) }

    val persistence by lazy {
        Persistence(
            LoggerFactory.getLogger(Persistence::class.java),
            cachedClient
        )
    }

    private val cachedClient by lazy {
        CachedSchemaRegistryClient(
            listOf(config.baseUrl),
            100,
            listOf(AvroSchemaProvider(), ProtobufSchemaProvider(), JsonSchemaProvider()),
            config.toMap(),
            null
        )
    }
    private val client by lazy { SchemaRegistryClient(cachedClient) }

    fun inject(configuration: Configuration) {
        config = configuration
    }
}
