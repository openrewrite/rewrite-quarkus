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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ConfigPropertiesToConfigMappingTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("quarkus-arc")
          ).recipe(new ConfigPropertiesToConfigMapping());
    }

    @Test
    void migrateAnnotatedInterfaces() {
        rewriteRun(
          java(
            """
              import io.quarkus.arc.config.ConfigProperties;

              @ConfigProperties(prefix = "greeting")
              interface MyConfiguration {
                  String message();

                  String name();
              }
              """,
            """
              import io.smallrye.config.ConfigMapping;

              @ConfigMapping(prefix = "greeting")
              interface MyConfiguration {
                  String message();

                  String name();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-quarkus/issues/24")
    @Disabled
    @Test
    void changeConfigClassType() {
        rewriteRun(
          java(
            """
              import io.quarkus.arc.config.ConfigProperties;

              @ConfigProperties(prefix = "greeting")
              class MyConfiguration {
                  String message;

                  String name;
              }
              """,
            """
              import io.smallrye.config.ConfigMapping;

              @ConfigMapping(prefix = "greeting")
              interface MyConfiguration {
                  String message();

                  String name();
              }
              """
          )
        );
    }
}
