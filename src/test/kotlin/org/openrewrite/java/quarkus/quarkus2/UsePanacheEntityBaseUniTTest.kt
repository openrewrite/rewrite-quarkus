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

class UsePanacheEntityBaseUniTTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("quarkus-hibernate-reactive-panache", "mutiny", "persistence-api")
        .build()

    override val recipe: Recipe
        get() = UsePanacheEntityBaseUniT()

    @Test
    fun replaceWithVoid() = assertChanged(
        before = """
            package org.openrewrite.example;

            import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
            import io.smallrye.mutiny.Uni;

            class Test {
                public static <T extends PanacheEntityBase> void method(T example) {
                    example.persist().await().indefinitely();
                    Uni<Void> e0 = example.persist();
                    Uni<Void> e1 = example.persistAndFlush();
                }
            }
        """,
        after = """
            package org.openrewrite.example;

            import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
            import io.smallrye.mutiny.Uni;

            class Test {
                public static <T extends PanacheEntityBase> void method(T example) {
                    example.persist().replaceWithVoid().await().indefinitely();
                    Uni<Void> e0 = example.persist().replaceWithVoid();
                    Uni<Void> e1 = example.persistAndFlush().replaceWithVoid();
                }
            }
        """
    )

}
