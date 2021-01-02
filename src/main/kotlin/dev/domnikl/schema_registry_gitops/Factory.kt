package dev.domnikl.schema_registry_gitops

import dev.domnikl.schema_registry_gitops.state.Applier
import dev.domnikl.schema_registry_gitops.state.Dumper
import dev.domnikl.schema_registry_gitops.state.Persistence
import dev.domnikl.schema_registry_gitops.state.Validator
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService
import org.slf4j.LoggerFactory

class Factory {
    fun createStateValidator(baseUrl: String): Validator {
        return Validator(createClient(baseUrl))
    }

    fun createStateApplier(baseUrl: String): Applier {
        return Applier(
            createClient(baseUrl),
            LoggerFactory.getLogger(Applier::class.java)
        )
    }

    fun createStateDumper(baseUrl: String): Dumper {
        return Dumper(createClient(baseUrl))
    }

    fun createStatePersistence() = Persistence()

    private fun createClient(baseUrl: String) = CachedSchemaRegistryClient(RestService(baseUrl), 100)
}
