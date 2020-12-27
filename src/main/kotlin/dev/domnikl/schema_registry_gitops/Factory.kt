package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService

class Factory {
    fun createStateValidator(baseUrl: String): StateValidator {
        return StateValidator(createClient(baseUrl))
    }

    fun createStateApplier(baseUrl: String): StateApplier {
        return StateApplier(createRestService(baseUrl), createClient(baseUrl))
    }

    fun createStateDumper(baseUrl: String): StateDumper {
        return StateDumper(
            createRestService(baseUrl)
        )
    }

    fun createStatePersistence() = StatePersistence()

    private fun createRestService(baseUrl: String) = RestService(baseUrl)
    private fun createClient(baseUrl: String) = CachedSchemaRegistryClient(createRestService(baseUrl), 100)
}
