package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException

class SchemaRegistryClient(private val schemaRegistryClient: SchemaRegistryClient) {
    fun subjects(): List<String> {
        return schemaRegistryClient.allSubjects.toList()
    }

    fun globalCompatibility(): Compatibility {
        return Compatibility.valueOf(schemaRegistryClient.getCompatibility(""))
    }

    fun updateGlobalCompatibility(compatibility: Compatibility): Compatibility {
        return Compatibility.valueOf(schemaRegistryClient.updateCompatibility("", compatibility.toString()))
    }

    fun compatibility(subject: String): Compatibility {
        return handleNotExisting {
            Compatibility.valueOf(schemaRegistryClient.getCompatibility(subject))
        } ?: Compatibility.NONE
    }

    fun updateCompatibility(subject: Subject): Compatibility {
        return Compatibility.valueOf(schemaRegistryClient.updateCompatibility(subject.name, subject.compatibility.toString()))
    }

    fun testCompatibility(subject: Subject): Boolean {
        return schemaRegistryClient.testCompatibility(subject.name, subject.schema)
    }

    fun getLatestSchema(subject: String): AvroSchema {
        return AvroSchema(schemaRegistryClient.getLatestSchemaMetadata(subject).schema)
    }

    fun create(subject: Subject): Int {
        return schemaRegistryClient.register(subject.name, subject.schema)
    }

    fun evolve(subject: Subject): Int {
        return schemaRegistryClient.register(subject.name, subject.schema)
    }

    fun version(subject: Subject): Int? {
        return handleNotExisting {
            schemaRegistryClient.getVersion(subject.name, subject.schema)
        }
    }

    private fun <V> handleNotExisting(f: () -> V?): V? {
        return try {
            f()
        } catch (e: RestClientException) {
            when (e.errorCode) {
                ERROR_CODE_SUBJECT_NOT_FOUND -> null
                ERROR_CODE_VERSION_NOT_FOUND -> null
                ERROR_CODE_SCHEMA_NOT_FOUND -> null
                else -> throw e
            }
        }
    }

    companion object {
        private const val ERROR_CODE_SUBJECT_NOT_FOUND = 40401
        private const val ERROR_CODE_VERSION_NOT_FOUND = 40402
        private const val ERROR_CODE_SCHEMA_NOT_FOUND = 40403
    }
}
