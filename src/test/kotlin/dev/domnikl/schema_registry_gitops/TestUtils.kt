package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.json.JsonSchema
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import org.apache.avro.Schema
import java.io.File

fun fromResources(name: String) = File(object {}.javaClass.classLoader.getResource(name)!!.toURI())
fun stringFromResources(name: String) = fromResources(name).readText()
fun avroFromResources(name: String) = AvroSchema(Schema.Parser().parse(fromResources(name)))
fun protoFromResources(name: String) = ProtobufSchema(stringFromResources(name))
fun jsonFromResources(name: String) = JsonSchema(stringFromResources(name))
