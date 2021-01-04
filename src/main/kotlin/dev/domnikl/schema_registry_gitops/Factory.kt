package dev.domnikl.schema_registry_gitops

import dev.domnikl.schema_registry_gitops.state.Applier
import dev.domnikl.schema_registry_gitops.state.Dumper
import dev.domnikl.schema_registry_gitops.state.Persistence
import dev.domnikl.schema_registry_gitops.state.Validator
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.slf4j.LoggerFactory

class Factory {
    lateinit var baseUrl: String

    val validator by lazy { Validator(client) }
    val applier by lazy { Applier(client, LoggerFactory.getLogger(Applier::class.java)) }
    val dumper by lazy { Dumper(client) }

    val persistence by lazy { Persistence(LoggerFactory.getLogger(Persistence::class.java)) }

    private val client by lazy { SchemaRegistryClient(CachedSchemaRegistryClient(baseUrl, 100)) }
}
