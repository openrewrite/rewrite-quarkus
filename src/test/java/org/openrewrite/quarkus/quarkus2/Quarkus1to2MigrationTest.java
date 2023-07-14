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
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class Quarkus1to2MigrationTest implements RewriteTest {

    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath(
              "quarkus-qute", "qute-core", "mongodb-driver-core",
              "quarkus-mongodb-client", "mongodb-driver-sync", "inject-api"
            ))
          .recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.quarkus.quarkus2")
            .build()
            .activateRecipes("org.openrewrite.quarkus.quarkus2.Quarkus1to2Migration")
          );
    }

    @DocumentExample
    @Test
    void quteResourcePathToLocation() {
        rewriteRun(
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
}
