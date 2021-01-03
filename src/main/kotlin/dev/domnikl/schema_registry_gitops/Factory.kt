package dev.domnikl.schema_registry_gitops

import dev.domnikl.schema_registry_gitops.state.Applier
import dev.domnikl.schema_registry_gitops.state.Dumper
import dev.domnikl.schema_registry_gitops.state.Persistence
import dev.domnikl.schema_registry_gitops.state.Validator
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.slf4j.LoggerFactory

class Factory {
    fun createValidator(baseUrl: String): Validator {
        return Validator(createClient(baseUrl))
    }

    fun createApplier(baseUrl: String): Applier {
        return Applier(
            createClient(baseUrl),
            LoggerFactory.getLogger(Applier::class.java)
        )
    }

    fun createDumper(baseUrl: String): Dumper {
        return Dumper(createClient(baseUrl))
    }

    fun createPersistence() = Persistence()

    private fun createClient(baseUrl: String) = SchemaRegistryClient(
        CachedSchemaRegistryClient(baseUrl, 100)
    )
}
