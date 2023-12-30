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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

class ChangeQuarkusPropertyKeyTest {

    @Nested
    class Properties implements RewriteTest {
        @Language("properties")
        private final String sourceProperties = """
          quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
          %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
          %staging,prod.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
                
          quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
          %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
          %staging,prod.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
          """;

        @Test
        void noChangesIfPropertyNotFound() {
            Recipe recipe = new ChangeQuarkusPropertyKey("quarkus\\.foo", "quarkus\\.bar", null, null, null, null);

            rewriteRun(spec -> spec.recipe(recipe), properties(sourceProperties, spec -> spec.path("src/main/resources/application.properties")));
        }

        @Test
        void changeDefaultProfile() {
            @Language("properties")
            String after = """
              quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=test
              %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
              %staging,prod.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
                            
              quarkus.hibernate-search-orm."unitname".indexing.plan.synchronization.strategy=test
              %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
              %staging,prod.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
              """;

            Recipe recipe = new ChangeQuarkusPropertyKey("quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy", "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy", null, null, null, null);

            rewriteRun(spec -> spec.recipe(recipe), properties(sourceProperties, after, spec -> spec.path("src/main/resources/application.properties")));
        }

        @Test
        void changeNamedProfile() {
            @Language("properties")
            String after = """
              quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
              %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
                            
              quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
              %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
              %prod.quarkus.hibernate-search-orm."unitname".indexing.plan.synchronization.strategy=test
              %staging.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
              %prod.quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=test
              %staging.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
              """;

            Recipe recipe = new ChangeQuarkusPropertyKey("quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy", "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy", null, "prod", false, null);

            rewriteRun(spec -> spec.recipe(recipe), properties(sourceProperties, after, spec -> spec.path("src/main/resources/application.properties")));
        }

        @Test
        void changeAllProfiles() {
            @Language("properties")
            String after = """
              quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=test
              %dev.quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=test
              %staging,prod.quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=test
                            
              quarkus.hibernate-search-orm."unitname".indexing.plan.synchronization.strategy=test
              %dev.quarkus.hibernate-search-orm."unitname".indexing.plan.synchronization.strategy=test
              %staging,prod.quarkus.hibernate-search-orm."unitname".indexing.plan.synchronization.strategy=test
              """;

            Recipe recipe = new ChangeQuarkusPropertyKey("quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy", "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy", null, null, true, null);

            rewriteRun(spec -> spec.recipe(recipe), properties(sourceProperties, after, spec -> spec.path("src/main/resources/application.properties")));
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
                  strategy: test
              unitname:
                automatic-indexing:
                  synchronization:
                    strategy: test
          '%dev':
            quarkus:
              hibernate-search-orm:
                automatic-indexing:
                  synchronization:
                    strategy: test
                unitname:
                  automatic-indexing:
                    synchronization:
                      strategy: test
          '%staging,prod':
            quarkus:
              hibernate-search-orm:
                automatic-indexing:
                  synchronization:
                    strategy: test
                unitname:
                  automatic-indexing:
                    synchronization:
                      strategy: test
          """;

        @Test
        void noChangesIfPropertyNotFound() {
            Recipe recipe = new ChangeQuarkusPropertyKey("quarkus\\.foo", "quarkus\\.bar", null, null, null, null);

            rewriteRun(spec -> spec.recipe(recipe), yaml(sourceYaml, spec -> spec.path("src/main/resources/application.yaml")));
        }

        @Test
        void changeDefaultProfile() {
            @Language("yml")
            String after = """
              quarkus:
                hibernate-search-orm:
                  indexing:
                    plan:
                      synchronization:
                        strategy: test
                  unitname:
                    indexing:
                      plan:
                        synchronization:
                          strategy: test
              '%dev':
                quarkus:
                  hibernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: test
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: test
              '%staging,prod':
                quarkus:
                  hibernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: test
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: test
              """;

            Recipe recipe = new ChangeQuarkusPropertyKey("quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy", "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy", null, null, null, null);

            rewriteRun(spec -> spec.recipe(recipe), yaml(sourceYaml, after, spec -> spec.path("src/main/resources/application.yaml")));
        }

        @Test
        void changeNamedProfile() {
            @Language("yml") String after = """
              quarkus:
                hibernate-search-orm:
                  automatic-indexing:
                    synchronization:
                      strategy: test
                  unitname:
                    automatic-indexing:
                      synchronization:
                        strategy: test
              '%dev':
                quarkus:
                  hibernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: test
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: test
              '%prod':
                quarkus:
                  hibernate-search-orm:
                    indexing:
                      plan:
                        synchronization:
                          strategy: test
                    unitname:
                      indexing:
                        plan:
                          synchronization:
                            strategy: test
              '%staging':
                quarkus:
                  hibernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: test
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: test
              """;

            Recipe recipe = new ChangeQuarkusPropertyKey("quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy", "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy", null, "prod", false, null);

            rewriteRun(spec -> spec.recipe(recipe), yaml(sourceYaml, after, spec -> spec.path("src/main/resources/application.yaml")));
        }

        @Test
        void changeAllProfiles() {
            @Language("yml")
            String after = """
              quarkus:
                hibernate-search-orm:
                  unitname:
                    indexing:
                      plan:
                        synchronization:
                          strategy: test
                  indexing:
                    plan:
                      synchronization:
                        strategy: test
              '%dev':
                quarkus:
                  hibernate-search-orm:
                    indexing:
                      plan:
                        synchronization:
                          strategy: test
                    unitname:
                      indexing:
                        plan:
                          synchronization:
                            strategy: test
              '%staging,prod':
                quarkus:
                  hibernate-search-orm:
                    indexing:
                      plan:
                        synchronization:
                          strategy: test
                    unitname:
                      indexing:
                        plan:
                          synchronization:
                            strategy: test
              """;

            Recipe recipe = new ChangeQuarkusPropertyKey("quarkus\\.hibernate-search-orm(\\..*)?\\.automatic-indexing\\.synchronization\\.strategy", "quarkus.hibernate-search-orm$1.indexing.plan.synchronization.strategy", null, null, true, null);

            rewriteRun(spec -> spec.recipe(recipe), yaml(sourceYaml, after, spec -> spec.path("src/main/resources/application.yaml")));
        }
    }
}
