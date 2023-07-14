/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.quarkus.quarkus2;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

class Quarkus1to2MigrationPropertiesTest implements RewriteTest {
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.quarkus.quarkus2")
          .build()
          .activateRecipes("org.openrewrite.quarkus.quarkus2.Quarkus1to2Migration")
        );
    }

    @DocumentExample
    @Test
    void smallryeJwt() {
        rewriteRun(
          properties(
            """
              smallrye.jwt.sign.key-location=/keys/signing
              smallrye.jwt.encrypt.key-location=/keys/encrypt
              """,
            """
              smallrye.jwt.sign.key.location=/keys/signing
              smallrye.jwt.encrypt.key.location=/keys/encrypt
              """
          )
        );
    }

    @Test
    void quarkusQuartz() {
        rewriteRun(
          properties(
            """
              quarkus.quartz.force-start=true
              quarkus.quartz.store-type=db
              """,
            """
              quarkus.quartz.start-mode=forced
              quarkus.quartz.store-type=jdbc-cmt
              """
          )
        );
    }

    @Test
    void quarkusNeo4j() {
        rewriteRun(
          properties(
            """
              quarkus.neo4j.pool.metrics-enabled=true
              """,
            """
              quarkus.neo4j.pool.metrics.enabled=true
              """
          )
        );
    }
}
