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
package org.openrewrite.java.quarkus

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class Quarkus1to113MigrationTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("mutiny", "reactive-streams")
        .build()

    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.quarkus")
        .build()
        .activateRecipes("org.openrewrite.java.quarkus.Quarkus1to1_13Migration")

    @Test
    fun changeMultiTransformAndByTakingFirst() = assertChanged(
        before = """
            import io.smallrye.mutiny.Multi;
            import io.smallrye.mutiny.Uni;

            import java.util.List;
            import java.time.Duration;

            class Test {
                public static Multi<String> greetings(int count, String name) {
                    return Multi.createFrom().ticks().every(Duration.ofMillis(1))
                            .onItem()
                            .transform(n -> "hello " + name + " -" + n)
                            .transform()
                            .byTakingFirstItems(count);
                }

                public static Uni<List<String>> collectItems(int count, String name) {
                    Multi<String> multi = greetings(count, name);
                    Uni<List<String>> uni = multi
                            .collectItems()
                            .asList();
                    return uni;
                }
            }
        """,
        after = """
            import io.smallrye.mutiny.Multi;
            import io.smallrye.mutiny.Uni;

            import java.util.List;
            import java.time.Duration;

            class Test {
                public static Multi<String> greetings(int count, String name) {
                    return Multi.createFrom().ticks().every(Duration.ofMillis(1))
                            .onItem()
                            .transform(n -> "hello " + name + " -" + n)
                            .select()
                            .first(count);
                }

                public static Uni<List<String>> collectItems(int count, String name) {
                    Multi<String> multi = greetings(count, name);
                    Uni<List<String>> uni = multi
                            .collect()
                            .asList();
                    return uni;
                }
            }
        """
    )

    @Nested
    inner class MutinyUniMultiApplyToTransform {
        @Test
        fun uniOnFailure() = assertChanged(
            before = """
                import io.smallrye.mutiny.Uni;

                class Test {
                    public static Uni<String> method(Uni<String> uni) {
                        return uni.onFailure().apply(f -> new RuntimeException("boom"));
                    }
                }
            """,
            after = """
                import io.smallrye.mutiny.Uni;

                class Test {
                    public static Uni<String> method(Uni<String> uni) {
                        return uni.onFailure().transform(f -> new RuntimeException("boom"));
                    }
                }
            """
        )

        @Test
        fun uniOnItem() = assertChanged(
            before = """
                import io.smallrye.mutiny.Uni;

                class Test {
                    public static Uni<String> method(Uni<String> uni) {
                        return uni.onItem().apply(String::toUpperCase);
                    }
                }
            """,
            after = """
                import io.smallrye.mutiny.Uni;

                class Test {
                    public static Uni<String> method(Uni<String> uni) {
                        return uni.onItem().transform(String::toUpperCase);
                    }
                }
            """
        )

        @Test
        fun uniOnItemOrFailure() = assertChanged(
            before = """
                import io.smallrye.mutiny.Uni;

                class Test {
                    public static Uni<String> method(Uni<String> uni) {
                        return uni.onItemOrFailure().apply((a, b) -> a + b);
                    }
                }
            """,
            after = """
                import io.smallrye.mutiny.Uni;

                class Test {
                    public static Uni<String> method(Uni<String> uni) {
                        return uni.onItemOrFailure().transform((a, b) -> a + b);
                    }
                }
            """
        )

        @Test
        fun uniOnNotNull() = assertChanged(
            before = """
                import io.smallrye.mutiny.Uni;

                class Test {
                    public static Uni<String> method(Uni<String> uni) {
                        return uni.onItem().ifNotNull().apply(String::toUpperCase);
                    }
                }
            """,
            after = """
                import io.smallrye.mutiny.Uni;

                class Test {
                    public static Uni<String> method(Uni<String> uni) {
                        return uni.onItem().ifNotNull().transform(String::toUpperCase);
                    }
                }
            """
        )

        @Test
        fun multiOnFailure() = assertChanged(
            before = """
                import io.smallrye.mutiny.Multi;
                
                class Test {
                    public static Multi<String> method(Multi<String> multi) {
                        return multi.onFailure().apply(f -> new RuntimeException("boom"));
                    }
                }
            """,
            after = """
                import io.smallrye.mutiny.Multi;
                
                class Test {
                    public static Multi<String> method(Multi<String> multi) {
                        return multi.onFailure().transform(f -> new RuntimeException("boom"));
                    }
                }
            """
        )

        @Test
        fun multiOnItem() = assertChanged(
            before = """
                import io.smallrye.mutiny.Multi;

                class Test {
                    public static Multi<String> method(Multi<String> multi) {
                        return multi.onItem().apply(String::toUpperCase);
                    }
                }
            """,
            after = """
                import io.smallrye.mutiny.Multi;

                class Test {
                    public static Multi<String> method(Multi<String> multi) {
                        return multi.onItem().transform(String::toUpperCase);
                    }
                }
            """
        )

    }

}
