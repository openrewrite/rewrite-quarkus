plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Migrate between major Quarkus versions. Automatically."

val rewriteVersion = if(project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}

dependencies {
    constraints {
        implementation("com.fasterxml.woodstox:woodstox-core:6.5.0") {
            because("Versions <= 6.3.1 contain vulnerabilities")
        }
    }

    compileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")

    implementation(platform("org.openrewrite:rewrite-bom:${rewriteVersion}"))
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-maven")
    implementation("org.openrewrite:rewrite-properties")

    implementation("org.openrewrite.recipe:rewrite-java-dependencies:${rewriteVersion}")

    runtimeOnly("org.openrewrite:rewrite-java-21")

    testImplementation("org.openrewrite:rewrite-test")

    testRuntimeOnly("org.openrewrite:rewrite-java-21")
    testRuntimeOnly("org.projectlombok:lombok:latest.release")
}

recipeDependencies {
    parserClasspath("io.quarkus:quarkus-core:3.+")

    testParserClasspath("io.quarkus:quarkus-arc:1.13.7.Final")
    testParserClasspath("io.quarkus:quarkus-grpc:1.13.7.Final")
    testParserClasspath("io.quarkus:quarkus-qute:1.13.7.Final")
    testParserClasspath("io.quarkus.qute:qute-core:1.13.7.Final")
    testParserClasspath("io.quarkus:quarkus-mongodb-client:1.13.7.Final")
    testParserClasspath("org.mongodb:mongodb-driver-core:4.2.3")
    testParserClasspath("org.mongodb:mongodb-driver-sync:4.2.3")
    testParserClasspath("io.quarkus:quarkus-mongodb-panache:1.13.7.Final")
    testParserClasspath("io.quarkus:quarkus-mongodb-panache-common:1.13.7.Final")
    testParserClasspath("io.quarkus:quarkus-hibernate-orm-panache:1.13.7.Final")
    testParserClasspath("io.quarkus:quarkus-hibernate-orm-panache-common:1.13.7.Final")
    testParserClasspath("io.quarkus:quarkus-hibernate-reactive-panache:1.13.7.Final")
    testParserClasspath("io.quarkus:quarkus-hibernate-reactive-panache-common:1.13.7.Final")
    testParserClasspath("io.smallrye.reactive:mutiny:0.14.0")
    testParserClasspath("org.reactivestreams:reactive-streams:1.0.3")
    testParserClasspath("io.smallrye.common:smallrye-common-annotation:1.6.0")
    testParserClasspath("jakarta.persistence:jakarta.persistence-api:2.2.3")
    testParserClasspath("javax:javaee-api:7.0")
    testParserClasspath("jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0")
    testParserClasspath("jakarta.inject:jakarta.inject-api:2.0.1")
    testParserClasspath("org.slf4j:slf4j-api:1.7.36")
}
