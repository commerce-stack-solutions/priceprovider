plugins {
    `java-library`
    id("io.commercestacksolutions.cdf-codegen")
}

dependencies {
    implementation(project(":commons"))

    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.3")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:4.1.0")
}

// Ensure this module is built before others that depend on it
// The plugin already wires compileJava to depend on generateClassesFromCDF
