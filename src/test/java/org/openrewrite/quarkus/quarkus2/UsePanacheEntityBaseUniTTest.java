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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UsePanacheEntityBaseUniTTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpathFromResources(new InMemoryExecutionContext(),
                "quarkus-hibernate-reactive-panache-1",
                "quarkus-hibernate-reactive-panache-common-1",
                "mutiny-0",
                "jakarta.persistence-api-2",
                "reactive-streams-1")
          )
          .recipe(new UsePanacheEntityBaseUniT());
    }

    @DocumentExample
    @Test
    void replaceWithVoid() {
        rewriteRun(
          java(
            """
              package org.openrewrite.example;

              import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
              import io.smallrye.mutiny.Uni;

              class Test {
                  public static <T extends PanacheEntityBase> void method(T example) {
                      example.persist().await().indefinitely();
                      Uni<Void> e0 = example.persist();
                      Uni<Void> e1 = example.persistAndFlush();
                  }
              }
              """,
            """
              package org.openrewrite.example;

              import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
              import io.smallrye.mutiny.Uni;

              class Test {
                  public static <T extends PanacheEntityBase> void method(T example) {
                      example.persist().replaceWithVoid().await().indefinitely();
                      Uni<Void> e0 = example.persist().replaceWithVoid();
                      Uni<Void> e1 = example.persistAndFlush().replaceWithVoid();
                  }
              }
              """
          )
        );
    }
}
