/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

class AddQuarkusPropertyTest implements RewriteTest {

    @DocumentExample
    @Test
    void addNestedIntoExisting() {
        rewriteRun(
          spec -> spec.recipe(new AddQuarkusProperty("quarkus.http.port", "9090", null, null, null)),
          //language=properties
          properties(
            """
              quarkus.http.root-path=/api
              """,
            """
              quarkus.http.port=9090
              quarkus.http.root-path=/api
              """,
            spec -> spec.path("src/main/resources/application.properties")
          ),
          //language=yaml
          yaml(
            """
              quarkus:
                http:
                  root-path: /api
              """,
            """
              quarkus:
                http:
                  root-path: /api
                  port: 9090
              """,
            spec -> spec.path("src/main/resources/application.yaml")
          )
        );
    }

    @Test
    void addPropertyToRoot() {
        rewriteRun(
          spec -> spec.recipe(new AddQuarkusProperty("fred", "fred", null, null, null)),
          //language=properties
          properties(
            """
              quarkus.http.port=9090
              """,
            """
              fred=fred
              quarkus.http.port=9090
              """,
            spec -> spec.path("src/main/resources/application.properties")
          ),
          //language=yaml
          yaml(
            """
              quarkus:
                http:
                  root-path: /api
              """,
            """
              quarkus:
                http:
                  root-path: /api
              fred: fred
              """,
            spec -> spec.path("src/main/resources/application.yaml")
          )
        );
    }

    @Test
    void addPropertyToRootWithProfile() {
        rewriteRun(
          spec -> spec.recipe(new AddQuarkusProperty("fred", "fred", null, "dev", null)),
          //language=properties
          properties(
            """
              quarkus.http.port=9090
              %dev.foo=bar
              """,
            """
              quarkus.http.port=9090
              %dev.foo=bar
              %dev.fred=fred
              """,
            spec -> spec.path("src/main/resources/application.properties")
          ),
          //language=yaml
          yaml(
            """
              quarkus:
                http:
                  root-path: /api
              """,
            """
              quarkus:
                http:
                  root-path: /api
              "%dev":
                fred: fred
              """,
            spec -> spec.path("src/main/resources/application.yaml")
          )
        );
    }

    @Test
    void propertyAlreadyExists() {
        rewriteRun(
          spec -> spec.recipe(new AddQuarkusProperty("fred", "fred", null, null, null)),
          //language=properties
          properties(
            """
              quarkus.http.port=9090
              fred=doNotChangeThis
              """,
            spec -> spec.path("src/main/resources/application.properties")
          ),
          //language=yaml
          yaml(
            """
              quarkus:
                http:
                  port: 9090
              fred: doNotChangeThis
              """,
            spec -> spec.path("src/main/resources/application.yaml")
          )
        );
    }

    @Test
    void addPropertyWithComment() {
        rewriteRun(
          spec -> spec.recipe(new AddQuarkusProperty("quarkus.http.root-path", "/api", "This property was added", null, null)),
          //language=properties
          properties(
            """
              quarkus.http.port=9090
              """,
            """
              quarkus.http.port=9090
              # This property was added
              quarkus.http.root-path=/api
              """,
            spec -> spec.path("src/main/resources/application.properties")
          ),
          //language=yaml
          yaml(
            """
              quarkus:
                http:
                  port: 9090
              """,
            """
              quarkus:
                http:
                  port: 9090
                  # This property was added
                  root-path: /api
              """,
            spec -> spec.path("src/main/resources/application.yaml")
          )
        );
    }

    @Test
    void makeChangeToMatchingFiles() {
        rewriteRun(
          spec -> spec.recipe(new AddQuarkusProperty("quarkus.http.root-path", "/api", "This property was added", null, null)),
          properties("# Sample empty properties file", s -> s.path("src/main/resources/test.properties")),
          //language=properties
          properties(
            """
              quarkus.http.port=9090
              """,
            """
              quarkus.http.port=9090
              # This property was added
              quarkus.http.root-path=/api
              """,
            s -> s.path("src/main/resources/application.properties")
          ),
          //language=yaml
          yaml(
            """
              quarkus:
                http:
                  port: 9090
              """,
            """
              quarkus:
                http:
                  port: 9090
                  # This property was added
                  root-path: /api
              """,
            s -> s.path("src/main/resources/application.yml")
          )
        );
    }

    @Test
    void doNotChangeToFilesThatDoNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new AddQuarkusProperty("quarkus.http.root-path", "/api", null, null, null)),
          properties(
            //language=properties
            """
              quarkus.http.port=9090
              """,
            s -> s.path("src/main/resources/application-test.properties")
          ),
          yaml(
            //language=yaml
            """
              quarkus:
                http:
                  port: 9090
              """,
            s -> s.path("src/main/resources/application-dev.yml")
          )
        );
    }

    @Test
    void makeChangeToMatchingFilesWithCustomPathExpression() {
        rewriteRun(
          spec -> spec.recipe(new AddQuarkusProperty("quarkus.http.root-path", "/api", "This property was added", null, List.of("**/custom.{properties,yaml,yml}"))),
          properties("# Sample empty properties file", s -> s.path("src/main/resources/test.properties")),
          //language=properties
          properties(
            """
              quarkus.http.port=9090
              """,
            """
              quarkus.http.port=9090
              # This property was added
              quarkus.http.root-path=/api
              """,
            s -> s.path("src/main/resources/custom.properties")
          ),
          //language=yaml
          yaml(
            """
              quarkus:
                http:
                  port: 9090
              """,
            """
              quarkus:
                http:
                  port: 9090
                  # This property was added
                  root-path: /api
              """,
            s -> s.path("src/main/resources/custom.yml")
          )
        );
    }
}
