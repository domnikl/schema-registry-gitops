# schema-registry-gitops

This is a pretty raw schema registry Gitops tool to handle subjects, compatibility levels and Avro schema registration for Confluence Schema Registry.

## TODOs

* use a logger to control output
* build as fat JAR
* build as Docker container
* UNIT & INTEGRATION TESTS!
* ktlint
* dump subcommand should write to a (given) YAML file
* inline schemas
* handle multiple YAML files (what about duplicate subjects?)
* argparse to control subcommands
* reading YAML input files + cli params where to find them
* support Protobuf & JSON Schema
* delete mode (should not be default) - deletes all unreferenced subjects
