package dev.domnikl.schemaregistrygitops

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference

data class Subject(
    val name: String,
    val compatibility: Compatibility?,
    val schema: ParsedSchema,
    val references: List<SchemaReference> = emptyList()
)
