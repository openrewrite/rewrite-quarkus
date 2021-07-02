/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.quarkus.quarkus2

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.properties.PropertiesRecipeTest

class Quarkus1to2MigrationPropertiesTest : PropertiesRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.quarkus.quarkus2")
        .build()
        .activateRecipes("org.openrewrite.java.quarkus.quarkus2.Quarkus1to2Migration")

    @Test
    fun smallryeJwt() = assertChanged(
        before = """
            smallrye.jwt.sign.key-location=/keys/signing
            smallrye.jwt.encrypt.key-location=/keys/encrypt
        """,
        after = """
            smallrye.jwt.sign.key.location=/keys/signing
            smallrye.jwt.encrypt.key.location=/keys/encrypt
        """
    )

    @Test
    fun quarkusQuartz() = assertChanged(
        before = """
            quarkus.quartz.force-start=true
            quarkus.quartz.store-type=db
        """,
        after = """
            quarkus.quartz.start-mode=forced
            quarkus.quartz.store-type=jdbc-cmt
        """
    )

    @Test
    fun quarkusNeo4j() = assertChanged(
        before = """
            quarkus.neo4j.pool.metrics-enabled=true
        """,
        after = """
            quarkus.neo4j.pool.metrics.enabled=true
        """
    )

}
