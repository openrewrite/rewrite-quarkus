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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

class DeleteQuarkusPropertyTest implements RewriteTest {

    @Nested
    class Properties implements RewriteTest {
        @Language("properties")
        private final String sourceProperties = """
          quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=read-sync
          %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=sync
          %staging,prod.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=async

          quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=read-sync
          %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=sync
          %staging,prod.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=async
          """;

        @Test
        void noChangesIfKeyNotFound() {
            rewriteRun(spec -> spec.recipe(new DeleteQuarkusProperty(
              "quarkus\\.foo",
              "bar", null, null, null)), properties(sourceProperties, spec -> spec.path("src/main/resources/application.properties")));
        }

        @Test
        void noChangesIfValueNotFound() {
            rewriteRun(spec -> spec.recipe(new DeleteQuarkusProperty(
              "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
              "foo", null, null, null)), properties(sourceProperties, spec -> spec.path("src/main/resources/application.properties")));
        }

        @Test
        void deleteValueOnDefaultProfileOnly() {
            @Language("properties")
            String after = """
              %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=sync
              %staging,prod.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=async
              %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=sync
              %staging,prod.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=async
              """;

            rewriteRun(
              spec -> spec.recipe(new DeleteQuarkusProperty(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                null, null, false, null)),
              properties(sourceProperties, after, spec -> spec.path("src/main/resources/application.properties"))
            );
        }

        @Test
        void deleteValueOnlyOnSpecificProfile() {
            @Language("properties")
            String after = """
              quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=read-sync
              %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=sync
              %prod.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=async
              %prod.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=async

              quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=read-sync
              %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=sync
              """;

            rewriteRun(
              spec -> spec.recipe(new DeleteQuarkusProperty(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                null, "staging", false, null)),
              properties(sourceProperties, after, spec -> spec.path("src/main/resources/application.properties"))
            );
        }

        @Test
        void deleteValueAllProfiles() {
            rewriteRun(
              spec -> spec.recipe(new DeleteQuarkusProperty(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                null, null, true, null)),
              properties(sourceProperties, "", spec -> spec.path("src/main/resources/application.properties"))
            );
        }

        @Test
        void deleteValueAllProfilesBecauseDefault() {
            rewriteRun(
              spec -> spec.recipe(new DeleteQuarkusProperty(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                null, null, null, null)),
              properties(sourceProperties, "", spec -> spec.path("src/main/resources/application.properties"))
            );
        }

        @Test
        void deleteValueOnCustomPath() {
            rewriteRun(
              spec -> spec.recipe(new DeleteQuarkusProperty(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                null, null, true, List.of("**/custom.{properties,yaml,yml}"))),
              properties(sourceProperties, "", spec -> spec.path("src/main/resources/custom.properties"))
            );
        }
    }

    @Nested
    class Yaml implements RewriteTest {
        @Language("yml")
        private final String sourceYaml = """
          quarkus:
            hibernate-search-orm:
              automatic-indexing:
                synchronization:
                  strategy: read-sync
              unitname:
                automatic-indexing:
                  synchronization:
                    strategy: read-sync
          '%dev':
            quarkus:
              hibernate-search-orm:
                automatic-indexing:
                  synchronization:
                    strategy: sync
                unitname:
                  automatic-indexing:
                    synchronization:
                      strategy: sync
          '%staging,prod':
            quarkus:
              hibernate-search-orm:
                automatic-indexing:
                  synchronization:
                    strategy: async
                unitname:
                  automatic-indexing:
                    synchronization:
                      strategy: async
          """;

        @Test
        void noChangesIfKeyNotFound() {
            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyValue(
                "quarkus\\.foo",
                "bar",
                null, null, null, null)),
              yaml(sourceYaml, spec -> spec.path("src/main/resources/application.yaml"))
            );
        }

        @Test
        void noChangesIfValueNotFound() {
            rewriteRun(
              spec -> spec.recipe(new DeleteQuarkusProperty(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                "foo", null, null, null)),
              yaml(sourceYaml, spec -> spec.path("src/main/resources/application.yaml"))
            );
        }

        @Test
        void deleteValueOnDefaultProfileOnly() {
            // intentional 2 blank lines because the compiler will remove the first one
            @Language("yml")
            String after = """


              '%dev':
                quarkus:
                  hibernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: sync
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: sync
              '%staging,prod':
                quarkus:
                  hibernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: async
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: async
              """;

            rewriteRun(
              spec -> spec.recipe(new DeleteQuarkusProperty(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                null, null, false, null)),
              yaml(sourceYaml, after, spec -> spec.path("src/main/resources/application.yaml"))
            );
        }

        @Test
        void deleteValueOnlyOnSpecificProfile() {
            @Language("yml") String after = """
              quarkus:
                hibernate-search-orm:
                  automatic-indexing:
                    synchronization:
                      strategy: read-sync
                  unitname:
                    automatic-indexing:
                      synchronization:
                        strategy: read-sync
              '%dev':
                quarkus:
                  hibernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: sync
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: sync
              '%prod':
                quarkus:
                  hibernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: async
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: async
              """;

            rewriteRun(
              spec -> spec.recipe(new DeleteQuarkusProperty(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                null, "staging", false, null)),
              yaml(sourceYaml, after, spec -> spec.path("src/main/resources/application.yaml"))
            );
        }

        @Test
        void deleteValueOnAllProfiles() {
            rewriteRun(
              spec -> spec.recipe(new DeleteQuarkusProperty(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                null, null, true, null)),
              yaml(sourceYaml, "", spec -> spec.path("src/main/resources/application.yaml"))
            );
        }

        @Test
        void deleteValueOnCustomPath() {
            rewriteRun(
              spec -> spec.recipe(new DeleteQuarkusProperty(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                null, null, true, List.of("**/custom.{properties,yaml,yml}"))),
              yaml(sourceYaml, "", spec -> spec.path("src/main/resources/custom.yaml"))
            );
        }
    }
}
