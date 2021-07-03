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
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class UseIdentifierOnDefaultKafkaBrokerTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("inject-api", "smallrye-common-annotation")
        .build()

    override val recipe: Recipe
        get() = UseIdentifierOnDefaultKafkaBroker()

    @Test
    fun namedAnnotationToIdentifier() = assertChanged(
        before = """
            package org.openrewrite.example;

            import javax.inject.Inject;
            import javax.inject.Named;
            import java.util.Map;

            class KafkaProviders {
                @Inject
                @Named("default-kafka-broker")
                Map<String, Object> config;
            }
        """,
        after = """
            package org.openrewrite.example;

            import io.smallrye.common.annotation.Identifier;

            import javax.inject.Inject;
            import java.util.Map;

            class KafkaProviders {
                @Inject
                @Identifier("default-kafka-broker")
                Map<String, Object> config;
            }
        """
    )

}
