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
            var after = """
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
            var after = """
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
            var after = """
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

        @Test
        void changeKeyOnAllProfilesBecauseDefault() {
            @Language("properties")
            var after = """
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
                null, null, null)),
              properties(sourceProperties, after, spec -> spec.path("src/main/resources/application.properties"))
            );
        }

        @Test
        void changeKeyOnCustomPath() {
            @Language("properties")
            var after = """
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
                null, false, List.of("**/custom.{properties,yaml,yml}"))),
              properties(sourceProperties, after, spec -> spec.path("src/main/resources/custom.properties"))
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
            var after = """
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
            @Language("yml") var after = """
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
            var after = """
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

        @Test
        void changeKeyOnCustomPath() {
            @Language("yml")
            var after = """
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
                null, true, List.of("**/custom.{properties,yaml,yml}"))),
              yaml(sourceYaml, after, spec -> spec.path("src/main/resources/custom.yaml"))
            );
        }

        @Test
        void changeKeyWithQueryEndingInWildcard() {
            @Language("yml")
            var after = """
              quarkus:
                hoobernate-search-orm:
                  automatic-indexing:
                    synchronization:
                      strategy: read-sync
                  unitname:
                    automatic-indexing:
                      synchronization:
                        strategy: read-sync
              '%dev':
                quarkus:
                  hoobernate-search-orm:
                    automatic-indexing:
                      synchronization:
                        strategy: sync
                    unitname:
                      automatic-indexing:
                        synchronization:
                          strategy: sync
              '%staging,prod':
                quarkus:
                  hoobernate-search-orm:
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
                "quarkus\\.hibernate-search-orm(.*)",
                "quarkus.hoobernate-search-orm$1",
                null, true, List.of("**/custom.{properties,yaml,yml}"))),
              yaml(sourceYaml, after, spec -> spec.path("src/main/resources/custom.yaml"))
            );
        }

        @Test
        void changeKeyWithSequenceValue() {
            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.micrometer\\.binder\\.vertx\\.match-patterns",
                "quarkus.micrometer.binder.http-server.match-patterns",
                null, null, null)),
              yaml(
                """
                  quarkus:
                    micrometer:
                      binder:
                        vertx:
                          match-patterns:
                            - /api/fruits/[0-9]+=/api/fruits/{id}
                            - /api/legumes/[0-9]+=/api/legumes/{id}
                  """,
                """
                  quarkus:
                    micrometer:
                      binder:
                        http-server:
                          match-patterns:
                            - /api/fruits/[0-9]+=/api/fruits/{id}
                            - /api/legumes/[0-9]+=/api/legumes/{id}
                  """,
                spec -> spec.path("src/main/resources/application.yaml")
              )
            );
        }

        @Test
        void changeKeyWithLiteralBlockValueOnProfile() {
            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.oidc\\.authentication\\.pkce-secret",
                "quarkus.oidc.authentication.state-secret",
                null, null, null)),
              yaml(
                """
                  '%dev':
                    quarkus:
                      oidc:
                        authentication:
                          pkce-secret: |
                            first line
                            second line
                  greeting: hello
                  """,
                """
                  greeting: hello
                  '%dev':
                    quarkus:
                      oidc:
                        authentication:
                          state-secret: |
                            first line
                            second line
                  """,
                spec -> spec.path("src/main/resources/application.yaml")
              )
            );
        }

        @Test
        void changeKeyWithLiteralBlockValueFollowedByOtherKeys() {
            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.oidc\\.authentication\\.pkce-secret",
                "quarkus.oidc.state-secret",
                null, null, null)),
              yaml(
                """
                  quarkus:
                    oidc:
                      authentication:
                        pkce-secret: |
                          first line
                          second line
                        cookie-path: /
                      tenant-id: main
                    log:
                      level: INFO
                  """,
                """
                  quarkus:
                    oidc:
                      authentication:
                        cookie-path: /
                      tenant-id: main
                      state-secret: |
                        first line
                        second line
                    log:
                      level: INFO
                  """,
                spec -> spec.path("src/main/resources/application.yaml")
              )
            );
        }

        @Test
        void changeKeyWithQuotedValue() {
            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.infinispan-client\\.auth-password",
                "quarkus.infinispan-client.password",
                null, null, null)),
              yaml(
                """
                  quarkus:
                    infinispan-client:
                      auth-password: "s3cr3t: #1"
                  """,
                """
                  quarkus:
                    infinispan-client:
                      password: "s3cr3t: #1"
                  """,
                spec -> spec.path("src/main/resources/application.yaml")
              )
            );
        }

        @Test
        void changeKeyUnderQuotedDottedKey() {
            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.hibernate-orm\\.(.+)\\.batch-fetch-size",
                "quarkus.hibernate-orm.$1.fetch.batch-size",
                null, null, null)),
              yaml(
                """
                  quarkus:
                    hibernate-orm:
                      "my.pu":
                        batch-fetch-size: 16
                  """,
                """
                  quarkus:
                    hibernate-orm:
                      "my.pu":
                        fetch:
                          batch-size: 16
                  """,
                spec -> spec.path("src/main/resources/application.yaml")
              )
            );
        }

        @Test
        void changeKeyUnderPlainDottedKey() {
            rewriteRun(
              spec -> spec.recipe(new ChangeQuarkusPropertyKey(
                "quarkus\\.http\\.cors$",
                "quarkus.http.cors.enabled",
                null, null, null)),
              yaml(
                """
                  quarkus.http.cors: true
                  """,
                """
                  quarkus:
                    http:
                      cors:
                        enabled: true
                  """,
                spec -> spec.path("src/main/resources/application.yaml")
              )
            );
        }
    }
}
