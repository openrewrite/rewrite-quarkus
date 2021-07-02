/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.quarkus.quarkus2

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class Quarkus1to2MigrationTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath(
            "quarkus-qute", "qute-core",
            "quarkus-mongodb-client", "mongodb-driver-sync", "inject-api"
        )
        .build()

    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.quarkus.quarkus2")
        .build()
        .activateRecipes("org.openrewrite.java.quarkus.quarkus2.Quarkus1to2Migration")

    @Test
    fun quteResourcePathToLocation() = assertChanged(
        before = """
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
        after = """
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

    @Test
    fun changeQuteCheckedTemplate() = assertChanged(
        before = """
            import io.quarkus.qute.TemplateInstance;
            import io.quarkus.qute.api.CheckedTemplate;

            @CheckedTemplate
            class Templates {
                public static native TemplateInstance hello(String name);
            }
        """,
        after = """
            import io.quarkus.qute.CheckedTemplate;
            import io.quarkus.qute.TemplateInstance;

            @CheckedTemplate
            class Templates {
                public static native TemplateInstance hello(String name);
            }
        """
    )

    @Test
    fun migrateQuarkusMongoClientName() = assertChanged(
        before = """
            import com.mongodb.client.MongoClient;
            import io.quarkus.mongodb.runtime.MongoClientName;

            import javax.inject.Inject;

            public class MongoStore {
                @Inject
                @MongoClientName("clientName")
                MongoClient mongoClient;
            }
        """,
        after = """
            import com.mongodb.client.MongoClient;
            import io.quarkus.mongodb.MongoClientName;

            import javax.inject.Inject;

            public class MongoStore {
                @Inject
                @MongoClientName("clientName")
                MongoClient mongoClient;
            }
        """
    )

}
