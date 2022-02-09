package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.ParsedSchema

data class Subject(val name: String, val compatibility: Compatibility?, val schema: ParsedSchema, val references: List<SubjectReference>?)
