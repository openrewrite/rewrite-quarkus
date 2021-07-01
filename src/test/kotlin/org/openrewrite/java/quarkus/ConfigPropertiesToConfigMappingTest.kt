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
package org.openrewrite.java.quarkus

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class ConfigPropertiesToConfigMappingTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("quarkus-arc")
        .build()

    override val recipe: Recipe
        get() = ConfigPropertiesToConfigMapping()

    @Test
    fun migrateAnnotatedInterfaces() = assertChanged(
        before = """
            import io.quarkus.arc.config.ConfigProperties;

            @ConfigProperties(prefix = "greeting")
            public interface MyConfiguration {
                String message();
                String name();
            }
        """,
        after = """
            import io.smallrye.config.ConfigMapping;

            @ConfigMapping(prefix = "greeting")
            public interface MyConfiguration {
                String message();
                String name();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite-quarkus/issues/24")
    @Disabled
    @Test
    fun changeConfigClassType() = assertChanged(
        before = """
            import io.quarkus.arc.config.ConfigProperties;

            @ConfigProperties(prefix = "greeting")
            public class MyConfiguration {
                String message;
                String name;
            }
        """,
        after = """
            import io.smallrye.config.ConfigMapping;

            @ConfigMapping(prefix = "greeting")
            public interface MyConfiguration {
                String message();
                String name();
            }
        """
    )
}