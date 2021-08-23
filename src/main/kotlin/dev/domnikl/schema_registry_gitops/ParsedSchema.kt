package dev.domnikl.schema_registry_gitops

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import com.squareup.wire.schema.internal.parser.ProtoFileElement
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.json.JsonSchema
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import org.apache.avro.Schema

private fun ParsedSchema.lines() = when (this) {
    is AvroSchema -> (this.rawSchema() as Schema).toString(true)
    is ProtobufSchema -> (this.rawSchema() as ProtoFileElement).toSchema()
    is JsonSchema -> this.toJsonNode().toPrettyString()
    else -> canonicalString()
}.lines()

fun ParsedSchema.diff(other: ParsedSchema): String {
    val generator = DiffRowGenerator.create()
        .showInlineDiffs(false)
        .inlineDiffByWord(false)
        .build()

    return generator.generateDiffRows(lines(), other.lines()).mapNotNull {
        when (it.tag) {
            DiffRow.Tag.INSERT -> "+ ${it.newLine}"
            DiffRow.Tag.DELETE -> "- ${it.oldLine}"
            DiffRow.Tag.CHANGE -> "- ${it.oldLine}\n+ ${it.newLine}"
            else -> "  ${it.oldLine}"
        }
    }.joinToString("\n")
}
