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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseReactivePanacheMongoEntityBaseUniTTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("quarkus-mongodb-panache", "mutiny", "persistence-api", "reactive-streams")
          )
          .recipe(new UseReactivePanacheMongoEntityBaseUniT());
    }

    @Test
    void replaceWithVoid() {
        rewriteRun(
          java(
            """
              package org.openrewrite.example;

              import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
              import io.smallrye.mutiny.Uni;

              class Test {
                  public static <T extends ReactivePanacheMongoEntityBase> void method(T example) {
                      example.persist().await().indefinitely();
                      Uni<Void> e0 = example.persist();
                      Uni<Void> e1 = example.update();
                      Uni<Void> e2 = example.persistOrUpdate();
                  }
              }
              """,
            """
              package org.openrewrite.example;

              import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
              import io.smallrye.mutiny.Uni;

              class Test {
                  public static <T extends ReactivePanacheMongoEntityBase> void method(T example) {
                      example.persist().replaceWithVoid().await().indefinitely();
                      Uni<Void> e0 = example.persist().replaceWithVoid();
                      Uni<Void> e1 = example.update().replaceWithVoid();
                      Uni<Void> e2 = example.persistOrUpdate().replaceWithVoid();
                  }
              }
              """
          )
        );
    }
}
