plugins {
    java
    kotlin("jvm") version "1.4.10"
}

group = "dev.domnikl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.confluent:kafka-schema-registry-client:6.0.1")
    testImplementation("junit", "junit", "4.12")
}
