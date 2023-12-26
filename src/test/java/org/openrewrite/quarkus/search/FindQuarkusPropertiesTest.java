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
package org.openrewrite.quarkus.search;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

class FindQuarkusPropertiesTest {

    private final String propertyKey = "quarkus.hibernate-search-orm(?:\\..*?)?.automatic-indexing.synchronization.strategy";

    @Test
    void validationOptions() {
        FindQuarkusProperties recipe = new FindQuarkusProperties("quarkus.http.port", null, null);
        assertThat(recipe.validate().isValid()).isTrue();

        recipe = new FindQuarkusProperties("quarkus.http.port", "dev", null);
        assertThat(recipe.validate().isValid()).isTrue();

        recipe = new FindQuarkusProperties("quarkus.http.port", "dev", true);
        assertThat(recipe.validate().isValid()).isFalse();
    }

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
        void nonExistingProperty() {
            rewriteRun(
              spec -> spec.recipe(new FindQuarkusProperties("quarkus.http.port", null, null)),
              //language=properties
              properties(sourceProperties)
            );
        }

        @Test
        void existingPropertyNoProfile() {
            rewriteRun(
              spec -> spec.recipe(new FindQuarkusProperties(propertyKey, null, null)),
              //language=properties
              properties(sourceProperties, """
                quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=~~>test
                %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
                %staging,prod.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
                      
                quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=~~>test
                %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
                %staging,prod.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
                """)
            );
        }

        @Test
        void existingPropertyAllProfiles() {
            rewriteRun(
              spec -> spec.recipe(new FindQuarkusProperties(propertyKey, null, true)),
              //language=properties
              properties(sourceProperties, """
                quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=~~>test
                %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=~~>test
                %staging,prod.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=~~>test
                      
                quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=~~>test
                %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=~~>test
                %staging,prod.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=~~>test
                """)
            );
        }

        @Test
        void existingPropertyNamedProfile() {
            rewriteRun(
              spec -> spec.recipe(new FindQuarkusProperties(propertyKey, "staging", false)),
              //language=properties
              properties(sourceProperties, """
                quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
                %dev.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=test
                %staging,prod.quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy=~~>test
                      
                quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
                %dev.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=test
                %staging,prod.quarkus.hibernate-search-orm."unitname".automatic-indexing.synchronization.strategy=~~>test
                """)
            );
        }
    }

    @Nested
    class Yaml implements RewriteTest {

        @Language("yml")
        private final String sourceYaml = """
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
        void nonExistingProperty() {
            rewriteRun(
              spec -> spec.recipe(new FindQuarkusProperties("quarkus.http.port", null, null)),
              //language=yaml
              yaml(sourceYaml)
            );
        }

        @Test
        void existingPropertyNoProfile() {
            rewriteRun(
              spec -> spec.recipe(new FindQuarkusProperties(propertyKey, null, null)),
              //language=yaml
              yaml(sourceYaml, """
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
                quarkus:
                  hibernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: ~~>test
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: ~~>test
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
                """)
            );
        }

        @Test
        void existingPropertyAllProfiles() {
            rewriteRun(
              spec -> spec.recipe(new FindQuarkusProperties(propertyKey, null, true)),
              //language=yaml
              yaml(sourceYaml, """
                '%dev':
                  quarkus:
                    hibernate-search-orm:
                      automatic-indexing:
                        synchronization:
                          strategy: ~~>test
                      unitname:
                        automatic-indexing:
                          synchronization:
                            strategy: ~~>test
                quarkus:
                  hibernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: ~~>test
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: ~~>test
                '%staging,prod':
                  quarkus:
                    hibernate-search-orm:
                      automatic-indexing:
                        synchronization:
                          strategy: ~~>test
                      unitname:
                        automatic-indexing:
                          synchronization:
                            strategy: ~~>test
                """)
            );
        }

        @Test
        void existingPropertyNamedProfile() {
            rewriteRun(
              spec -> spec.recipe(new FindQuarkusProperties(propertyKey, "staging", false)),
              //language=yaml
              yaml(sourceYaml, """
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
                          strategy: ~~>test
                      unitname:
                        automatic-indexing:
                          synchronization:
                            strategy: ~~>test
                """)
            );
        }
    }
}