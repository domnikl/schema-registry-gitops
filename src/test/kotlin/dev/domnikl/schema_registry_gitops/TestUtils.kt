package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import org.apache.avro.Schema
import java.io.File

fun fromResources(name: String) = File(object {}.javaClass.classLoader.getResource(name)!!.toURI())
fun stringFromResources(name: String) = fromResources(name).readText()
fun schemaFromResources(name: String) = AvroSchema(Schema.Parser().parse(fromResources(name)))
