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
package org.openrewrite.quarkus;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MultiTransformHotStreamToMultiHotStreamTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("mutiny", "reactive-streams")
          )
          .recipe(new MultiTransformHotStreamToMultiHotStream());
    }

    @Test
    void replaceTransform() {
        rewriteRun(
          java(
            """
              import io.smallrye.mutiny.Multi;
              import io.smallrye.mutiny.groups.MultiCollect;

              import java.time.Duration;

              class Test {
                  public static MultiCollect<Long> hotStreamGreetings(int count, String name) {
                      return Multi.createFrom().ticks().every(Duration.ofMillis(1))
                              .transform()
                              .toHotStream()
                              .collect();
                  }
              }
              """,
            """
              import io.smallrye.mutiny.Multi;
              import io.smallrye.mutiny.groups.MultiCollect;

              import java.time.Duration;

              class Test {
                  public static MultiCollect<Long> hotStreamGreetings(int count, String name) {
                      return Multi.createFrom().ticks().every(Duration.ofMillis(1))
                              .toHotStream()
                              .collect();
                  }
              }
              """
          )
        );
    }
}
