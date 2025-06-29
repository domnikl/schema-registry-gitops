import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "2.1.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.10"
    jacoco
}

group = "dev.domnikl"
version = "1.12.0"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    implementation("info.picocli:picocli:4.7.7")

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("ch.qos.logback:logback-core:1.5.18")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.1")
    constraints {
        implementation("com.google.code.gson:gson:2.13.1") {
            because("CVE-2022-25647")
        }
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("CVE-2023-42503")
        }
    }

    implementation("io.confluent:kafka-schema-registry-client:7.9.2")
    implementation("io.confluent:kafka-protobuf-serializer:7.9.2")
    implementation("io.confluent:kafka-json-schema-serializer:7.9.2")
    implementation("com.github.everit-org.json-schema:org.everit.json.schema:1.14.4")

    implementation("io.github.java-diff-utils:java-diff-utils:4.15")

    testImplementation(platform("org.junit:junit-bom:5.13.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.14.4")
    testImplementation("com.github.stefanbirkner:system-rules:1.19.0")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.majorVersion
        }
    }

    val versionTxt = register("versionTxt") {
        doLast {
            val outputDir = File("${sourceSets.main.get().output.resourcesDir}/version.txt")

            if (outputDir.exists()) outputDir.writeText("${project.version}")
        }
    }

    withType<ShadowJar> {
        dependsOn(versionTxt)
        archiveFileName.set("schema-registry-gitops.jar")
    }

    withType<Jar> {
        dependsOn(versionTxt)

        manifest {
            attributes["Main-Class"] = "dev.domnikl.schemaregistrygitops.MainKt"
        }
    }
}
