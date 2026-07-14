plugins {
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "io.commercestacksolutions"
    version = "0.0.1-SNAPSHOT"
}

tasks.register("bootRun") {
    group = "application"
    description = "Delegates to :io.commercestacksolutions.priceproviderservice:bootRun"
    dependsOn(":io.commercestacksolutions.priceproviderservice:bootRun")
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    repositories {
        mavenCentral()
    }
}
