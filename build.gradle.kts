import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.TaskTriggersConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version "0.9"
    jacoco
}

group = "dev.domnikl"
version = "1.4.0"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("info.picocli:picocli:4.6.2")

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("ch.qos.logback:logback-core:1.2.10")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")

    implementation("io.confluent:kafka-schema-registry-client:6.0.1")
    implementation("io.confluent:kafka-protobuf-serializer:6.0.1")
    implementation("io.confluent:kafka-json-schema-serializer:6.0.1")
    implementation("com.github.everit-org.json-schema:org.everit.json.schema:1.12.1")

    implementation("io.github.java-diff-utils:java-diff-utils:4.11")

    implementation("com.google.dagger:dagger:2.40.5")
    annotationProcessor("com.google.dagger:dagger-compiler:2.40.5")
    kapt("com.google.dagger:dagger-compiler:2.40.5")

    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.12.2")
    testImplementation("com.github.stefanbirkner:system-rules:1.19.0")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.majorVersion
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
            attributes["Main-Class"] = "dev.domnikl.schema_registry_gitops.MainKt"
        }
    }
}

idea {
    project {
        this as ExtensionAware
        configure<ProjectSettings> {
            this as ExtensionAware
            configure<TaskTriggersConfig> {
                afterSync(tasks.findByName("ktlintApplyToIdea"))
            }
        }
    }
}
