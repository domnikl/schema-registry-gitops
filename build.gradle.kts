import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "dev.domnikl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("info.picocli:picocli:4.5.2")

    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback:logback-core:1.2.3")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.0")

    implementation("io.confluent:kafka-schema-registry-client:6.0.1")
    testImplementation("junit", "junit", "4.12")
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "dev.domnikl.schema_registry_gitops.MainKt"
    }
}
