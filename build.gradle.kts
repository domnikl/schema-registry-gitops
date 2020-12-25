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

    implementation("info.picocli:picocli:4.5.2")

    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback:logback-core:1.2.3")

    implementation("io.confluent:kafka-schema-registry-client:6.0.1")
    testImplementation("junit", "junit", "4.12")
}
