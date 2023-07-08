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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseIdentifierOnDefaultKafkaBrokerTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("inject-api", "smallrye-common-annotation")
          )
          .recipe(new UseIdentifierOnDefaultKafkaBroker());
    }

    @DocumentExample
    @Test
    void namedAnnotationToIdentifier() {
        rewriteRun(
          java(
            """
              package org.openrewrite.example;
                      
              import javax.inject.Inject;
              import javax.inject.Named;
              import java.util.Map;
                      
              class KafkaProviders {
                  @Inject
                  @Named("default-kafka-broker")
                  Map<String, Object> config;
              }
              """,
            """
              package org.openrewrite.example;

              import io.smallrye.common.annotation.Identifier;

              import javax.inject.Inject;
              import java.util.Map;

              class KafkaProviders {
                  @Inject
                  @Identifier("default-kafka-broker")
                  Map<String, Object> config;
              }
              """
          )
        );
    }
}
