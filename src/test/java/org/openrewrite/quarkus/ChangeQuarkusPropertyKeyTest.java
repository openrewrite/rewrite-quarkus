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

import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

class ChangeQuarkusPropertyKeyTest {

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
            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.foo",
                "quarkus\\.bar",
                null, null, null)),
              properties(sourceProperties, spec -> spec.path("src/main/resources/application.properties"))
            );
        }

        @Test
        void changeKeyOnDefaultProfileOnly() {
            @Language("properties")
            String after = """
              quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=read-sync
              %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=sync
              %staging,prod.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=async
                            
              quarkus.hibernate-search-orm."unitname".indexing.plan.synchronization.strategy=read-sync
              %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=sync
              %staging,prod.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=async
              """;

            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy",
                null, false, null)),
              properties(sourceProperties, after, spec -> spec.path("src/main/resources/application.properties"))
            );
        }

        @Test
        void changeKeyOnlyOnSpecificProfile() {
            @Language("properties")
            String after = """
              quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=read-sync
              %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=sync
              %prod.quarkus.hibernate-search-orm."unitname".indexing.plan.synchronization.strategy=async
              %prod.quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=async
              %staging.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=async
              %staging.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=async
              
              quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=read-sync
              %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=sync
              """;

            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy",
                "prod", false, null)),
              properties(sourceProperties, after, spec -> spec.path("src/main/resources/application.properties"))
            );
        }

        @Test
        void changeKeyOnAllProfiles() {
            @Language("properties")
            String after = """
              quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=read-sync
              %dev.quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=sync
              %staging,prod.quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=async
                            
              quarkus.hibernate-search-orm."unitname".indexing.plan.synchronization.strategy=read-sync
              %dev.quarkus.hibernate-search-orm."unitname".indexing.plan.synchronization.strategy=sync
              %staging,prod.quarkus.hibernate-search-orm."unitname".indexing.plan.synchronization.strategy=async
              """;

            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy",
                null, true, null)),
              properties(sourceProperties, after, spec -> spec.path("src/main/resources/application.properties"))
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
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.foo",
                "quarkus\\.bar",
                null, null, null)),
              yaml(sourceYaml, spec -> spec.path("src/main/resources/application.yaml"))
            );
        }

        @Test
        void changeKeyOnDefaultProfileOnly() {
            @Language("yml")
            String after = """
              quarkus:
                hibernate-search-orm:
                  indexing:
                    plan:
                      synchronization:
                        strategy: read-sync
                  unitname:
                    indexing:
                      plan:
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

            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy",
                null, false, null)),
              yaml(sourceYaml, after, spec -> spec.path("src/main/resources/application.yaml"))
            );
        }

        @Test
        void changeKeyOnlyOnSpecificProfile() {
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
                    indexing:
                      plan:
                        synchronization:
                          strategy: async
                    unitname:
                      indexing:
                        plan:
                          synchronization:
                            strategy: async
              '%staging':
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
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy",
                "prod", false, null)),
              yaml(sourceYaml, after, spec -> spec.path("src/main/resources/application.yaml"))
            );
        }

        @Test
        void changeKeyOnAllProfiles() {
            @Language("yml")
            String after = """
              quarkus:
                hibernate-search-orm:
                  indexing:
                    plan:
                      synchronization:
                        strategy: read-sync
                  unitname:
                    indexing:
                      plan:
                        synchronization:
                          strategy: read-sync
              '%dev':
                quarkus:
                  hibernate-search-orm:
                    indexing:
                      plan:
                        synchronization:
                          strategy: sync
                    unitname:
                      indexing:
                        plan:
                          synchronization:
                            strategy: sync
              '%staging,prod':
                quarkus:
                  hibernate-search-orm:
                    indexing:
                      plan:
                        synchronization:
                          strategy: async
                    unitname:
                      indexing:
                        plan:
                          synchronization:
                            strategy: async
              """;

            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy",
                "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy",
                null, true, null)),
              yaml(sourceYaml, after, spec -> spec.path("src/main/resources/application.yaml"))
            );
        }
    }
}
