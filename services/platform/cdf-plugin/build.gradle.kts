plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
}

gradlePlugin {
    plugins {
        create("cdfCodeGeneration") {
            id = "io.commercestacksolutions.cdf-codegen"
            implementationClass = "io.commercestacksolutions.codegen.CdfCodeGenerationPlugin"
        }
    }
}
