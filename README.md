# schema-registry-gitops

![build](https://github.com/domnikl/schema-registry-gitops/workflows/build/badge.svg)
![Docker Pulls](https://img.shields.io/docker/pulls/domnikl/schema-registry-gitops)
<a href="https://codeclimate.com/github/domnikl/schema-registry-gitops/maintainability"><img src="https://api.codeclimate.com/v1/badges/2e87990ad7212a273b49/maintainability" /></a>
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Manages subjects, compatibility levels and schema registration for Confluence Schema Registry through a desired state file.

## Overview

Schema Registry GitOps is an Infrastructure as Code tool that applies a desired state configured through simple YAML and
Avro Schema files to a Confluent Schema Registry. This way you can keep a version control history of your schemas and 
use all your favorite tools to review, merge and deploy schemas in your CI/CD pipeline.

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

In order to get help for a specific command, try `schema-registry-gitops <command> -h`.

## State file

The desired state is managed using this YAML definition:

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

## Development

Docker is used to build and test schema-registry-gitops for development.

```shell
# test & build
docker build -t domnikl/schema-registry-gitops .

# run it in Docker
docker run -v ./examples:/data domnikl/schema-registry-gitops validate --registry http://localhost:8081 /data/schema-registry.yml
```

## Acknowledgement

Schema Registry GitOps was born late in 2020 while being heavily inspired by [Shawn Seymour](https://github.com/devshawn) and his excellent [kafka-gitops](https://github.com/devshawn/kafka-gitops)! Much ❤ to [Confluent](https://www.confluent.io/) for building Schema Registry and an amazing client lib, I am really just standing on the shoulders of giants here.
