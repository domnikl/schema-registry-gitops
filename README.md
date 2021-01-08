# schema-registry-gitops

[![build](https://github.com/domnikl/schema-registry-gitops/workflows/build/badge.svg)](https://github.com/domnikl/schema-registry-gitops/actions)
[![Docker Pulls](https://img.shields.io/docker/pulls/domnikl/schema-registry-gitops)](https://hub.docker.com/repository/docker/domnikl/schema-registry-gitops)
<a href="https://codeclimate.com/github/domnikl/schema-registry-gitops/maintainability"><img src="https://api.codeclimate.com/v1/badges/2e87990ad7212a273b49/maintainability" /></a>
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Manages subjects, compatibility levels and schema registration for [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html) through applying a desired state file.

## Overview

Schema Registry GitOps is an Infrastructure as Code tool that applies a desired state configured through simple YAML and
Avro/Protobuf/JSON Schema files to a schema registry. That way you can keep a version control history of your
schemas and use all your favorite tools to validate, review, merge and evolve schemas in your CI/CD pipeline.

## Usage

```
Usage: schema-registry-gitops [-hvV] [--properties=<propertiesFilePath>]
                              [-r=<baseUrl>] [COMMAND]
Manages schema registries through Infrastructure as Code
  -h, --help                 Show this help message and exit.
      --properties=<propertiesFilePath>
                             a Java Properties file for client configuration (optional)
  -r, --registry=<baseUrl>   schema registry endpoint, overwrites 'schema.registry.url' from properties
  -v, --verbose              enable verbose logging
  -V, --version              Print version information and exit.
Commands:
  help      Displays help information about the specified command
  validate  validate schemas, should be used before applying changes
  apply     applies the state to the given schema registry
  dump      prints the current state
```

In order to get help for a specific command, try `schema-registry-gitops <command> -h`.

## Using Docker

`schema-registry-gitops` is available through [docker hub](https://hub.docker.com/repository/docker/domnikl/schema-registry-gitops), so running it in a container is as easy as:

```sh
docker run -v "$(pwd)/examples":/data domnikl/schema-registry-gitops validate --registry http://localhost:8081 /data/schema-registry.yml
```

Please keep in mind that using a tagged release may be a good idea.

## State file

The desired state is managed using this YAML schema:

```yaml
# sets global compatibility level (optional)
compatibility: FULL_TRANSITIVE
subjects:
  # a subject that references a file for the schema definition
  - name: my-new-subject-referencing-a-schema-file
    # sets compatibility level for this subject (optional)
    compatibility: BACKWARD
    # file references are always relative to the given (this) YAML file
    file: my-actual-schema.avsc
    # AVRO is the default type and can safely be omitted
    type: AVRO

  # another example: instead of referencing a file, it is also possible
  # to define the schema directly here, which is Protocol Buffers here (note explicit type here)
  - name: my-new-inline-schema-subject
    schema: 'syntax = "proto3";
        package com.acme;
        
        message OtherRecord {
          int32 an_id = 1;
        }'
    type: PROTOBUF
```

### compatibility

Supported `compatibility` values are:
* `NONE`
* `FORWARD`
* `BACKWARD`
* `FULL`
* `FORWARD_TRANSITIVE`
* `BACKWARD_TRANSITIVE`
* `FULL_TRANSITIVE`

### file, schema

Either one of `file` (recommended) or `schema` must be set. The former contains a path to a schema file while the latter can be set
to a string containing the schema.

### type

Supported `type` values are: `AVRO`, `PROTOBUF` and `JSON`.

## Configuration .properties

Configuration properties are being used to connect to the Schema Registry. The most common use case to use them
instead of just supplying `--registry` is to use SSL. The example below uses client certificates authentication.

```properties
schema.registry.url=https://localhost:8081
security.protocol=SSL
schema.registry.ssl.truststore.location=truststore.jks
schema.registry.ssl.truststore.password=<secret>
schema.registry.ssl.keystore.location=keystore.jks
schema.registry.ssl.keystore.password=<secret>
schema.registry.ssl.key.password=<secret>
```

## Development

Docker is used to build and test `schema-registry-gitops` for development.

```sh
# test & build
docker build -t domnikl/schema-registry-gitops .

# run it in Docker
docker run -v ./examples:/data domnikl/schema-registry-gitops validate --registry http://localhost:8081 /data/schema-registry.yml
```

## Acknowledgement

Schema Registry GitOps was born late in 2020 while being heavily inspired by [Shawn Seymour](https://github.com/devshawn) and his excellent [kafka-gitops](https://github.com/devshawn/kafka-gitops)! Much ❤ to [Confluent](https://www.confluent.io/) for building Schema Registry and an amazing client lib, I am really just standing on the shoulders of giants here.
