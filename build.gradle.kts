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

    runtimeOnly("org.openrewrite:rewrite-java-17")

    testImplementation("org.openrewrite:rewrite-test")

    testImplementation("javax.xml.ws:jaxws-api:2.3.1")
    testImplementation("jakarta.xml.ws:jakarta.xml.ws-api:3.0.0")

    testImplementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
    testImplementation("jakarta.xml.bind:jakarta.xml.bind-api:3.0.0")

    testImplementation("javax:javaee-api:7.0")


    testRuntimeOnly("org.openrewrite:rewrite-java-17")

    testRuntimeOnly("io.quarkus:quarkus-grpc:1.13.+")
    testRuntimeOnly("io.quarkus:quarkus-qute:1.13.+")
    testRuntimeOnly("io.quarkus:quarkus-mongodb-client:1.13.+")
    testRuntimeOnly("io.quarkus:quarkus-mongodb-panache:1.13.+")
    testRuntimeOnly("io.quarkus:quarkus-hibernate-orm-panache:1.13.+")
    testRuntimeOnly("io.quarkus:quarkus-hibernate-reactive-panache:1.13.+")
    testRuntimeOnly("io.smallrye.reactive:mutiny:0.12.+")

    testRuntimeOnly("jakarta.inject:jakarta.inject-api:2.0.1")
    testRuntimeOnly("jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0")
    testRuntimeOnly("org.projectlombok:lombok:latest.release")
}

recipeDependencies {
    parserClasspath("io.quarkus:quarkus-core:3.+")
}
