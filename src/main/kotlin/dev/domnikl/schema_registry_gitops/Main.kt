package dev.domnikl.schema_registry_gitops

import dev.domnikl.schema_registry_gitops.command.Apply
import dev.domnikl.schema_registry_gitops.command.Dump
import dev.domnikl.schema_registry_gitops.command.Validate
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService
import org.apache.avro.Schema
import java.io.File
import java.lang.IllegalArgumentException
import kotlin.system.exitProcess


fun main() {
    val restService = RestService("http://localhost:8081")
    val client = CachedSchemaRegistryClient(restService, 100)

    val state = State(
        compatibility = Compatibility.FULL_TRANSITIVE,
        listOf(
            Subject("foo", Compatibility.FORWARD, AvroSchema(Schema.Parser().parse(File("examples/foo.avsc"))))
        )
    )

    val subCommand = "apply"

    val command = when (subCommand) {
        "validate" -> Validate(client, state)
        "apply" -> Apply(restService, client, state)
        "dump" -> Dump(restService)
        else -> throw IllegalArgumentException("Unknown command: $subCommand")
    }

    exitProcess(command.execute())
}
