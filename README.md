# schema-registry-gitops

This is a pretty raw schema registry Gitops tool to handle subjects, compatibility levels and Avro schema registration for Confluence Schema Registry.

## Usage

```
Usage: schema-registry-gitops [-hvV] [-r=<baseUrl>] [COMMAND]
Manages schema registries through Infrastructure as Code
  -h, --help                 Show this help message and exit.
  -r, --registry=<baseUrl>   schema registry HTTP endpoint
  -v, --verbose              enable verbose logging
  -V, --version              Print version information and exit.
Commands:
  validate  validate schemas, should be used before applying changes
  apply     applies the state to the given schema registry
  dump      prints the current state
```

## State file

The desired state is managed using this YAML definition:

```yaml
# sets default compatibility level, optional
compatibility: FULL_TRANSITIVE
subjects:
  # a subject that references a file for the schema definition
  - name: my-new-subject-referencing-a-schema-file
    # sets compatibility level for this subject, optional
    compatibility: BACKWARD
    # file references are always relative to this YAML file
    file: my-actual-schema.avsc

  # another example: instead of referencing a file, it is also possible
  # to define the schema directly here
  - name: my-new-inline-schema-subject
    schema: '{
       "type": "record",
       "name": "HelloWorld",
       "namespace": "dev.domnikl.schema_registry_gitops",
       "fields": [
         {
           "name": "greeting",
           "type": "string"
         }
       ]
  }'
```

## TODOs

* UNIT & INTEGRATION TESTS!
* build as Docker container
* ktlint
* extend logging: create schema version IDs
* handle multiple YAML files (what about duplicate subjects?)
* support Protobuf & JSON Schema
* delete mode for apply (should not be default) - deletes all unreferenced subjects
* purge subcommand to delete all subjects (-f)
* handle multiple schema versions per subject
