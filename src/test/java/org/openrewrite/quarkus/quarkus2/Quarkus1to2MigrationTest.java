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
import static org.openrewrite.maven.Assertions.pomXml;

class Quarkus1to2MigrationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath(
              "quarkus-qute", "qute-core", "mongodb-driver-core",
              "quarkus-mongodb-client", "mongodb-driver-sync", "inject-api"
            ))
          .recipeFromResource("/META-INF/rewrite/quarkus.yml","org.openrewrite.quarkus.quarkus2.Quarkus1to2Migration");
    }

    @DocumentExample
    @Test
    void quteResourcePathToLocation() {
        rewriteRun(
          //language=java
          java(
            """
              import io.quarkus.qute.api.ResourcePath;
              import io.quarkus.qute.Template;

              class ReportGenerator {
                  @ResourcePath("reports/v1/report_01")
                  Template report;

                  void generate() {
                      String result = report
                              .data("samples", new Object())
                              .render();
                  }
              }
              """,
            """
              import io.quarkus.qute.Location;
              import io.quarkus.qute.Template;

              class ReportGenerator {
                  @Location("reports/v1/report_01")
                  Template report;

                  void generate() {
                      String result = report
                              .data("samples", new Object())
                              .render();
                  }
              }
              """
          )
        );
    }


    @Test
    void changeQuteCheckedTemplate() {
        rewriteRun(
          //language=java
          java(
            """
              import io.quarkus.qute.TemplateInstance;
              import io.quarkus.qute.api.CheckedTemplate;

              @CheckedTemplate
              class Templates {
                  public static native TemplateInstance hello(String name);
              }
              """,
            """
              import io.quarkus.qute.CheckedTemplate;
              import io.quarkus.qute.TemplateInstance;

              @CheckedTemplate
              class Templates {
                  public static native TemplateInstance hello(String name);
              }
              """
          )
        );
    }


    @Test
    void migrateQuarkusMongoClientName() {
        rewriteRun(
          //language=java
          java(
            """
              import com.mongodb.client.MongoClient;
              import io.quarkus.mongodb.runtime.MongoClientName;

              import javax.inject.Inject;

              class MongoStore {
                  @Inject
                  @MongoClientName("clientName")
                  MongoClient mongoClient;
              }
              """,
            """
              import com.mongodb.client.MongoClient;
              import io.quarkus.mongodb.MongoClientName;

              import javax.inject.Inject;

              class MongoStore {
                  @Inject
                  @MongoClientName("clientName")
                  MongoClient mongoClient;
              }
              """
          )
        );
    }


    @Test
    void upgradeQuarkusUniverseBom() {
        rewriteRun(
          //language=XML
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-bom</artifactId>
                      <version>1.13.7.Final</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-bom</artifactId>
                      <version>2.16.12.Final</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }
    @Test
    void upgradeQuarkusUniverseParent() {
        rewriteRun(
          //language=XML
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>io.quarkus</groupId>
                  <artifactId>quarkus-universe-bom</artifactId>
                  <version>1.13.7.Final</version>
                  <relativePath />
                </parent>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>io.quarkus</groupId>
                  <artifactId>quarkus-universe-bom</artifactId>
                  <version>2.16.12.Final</version>
                  <relativePath />
                </parent>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }
}
