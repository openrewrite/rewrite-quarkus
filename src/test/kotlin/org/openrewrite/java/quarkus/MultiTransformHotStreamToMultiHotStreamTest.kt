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

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class MultiTransformHotStreamToMultiHotStreamTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("mutiny", "reactive-streams")
        .build()

    override val recipe: Recipe
        get() = MultiTransformHotStreamToMultiHotStream()

    @Test
    fun replaceTransform() = assertChanged(
        before = """
            import io.smallrye.mutiny.Multi;
            import io.smallrye.mutiny.groups.MultiCollect;

            import java.time.Duration;

            class Test {
                public static MultiCollect<Long> hotStreamGreetings(int count, String name) {
                    return Multi.createFrom().ticks().every(Duration.ofMillis(1))
                            .transform()
                            .toHotStream()
                            .collect();
                }
            }
        """,
        after = """
            import io.smallrye.mutiny.Multi;
            import io.smallrye.mutiny.groups.MultiCollect;

            import java.time.Duration;

            class Test {
                public static MultiCollect<Long> hotStreamGreetings(int count, String name) {
                    return Multi.createFrom().ticks().every(Duration.ofMillis(1))
                            .toHotStream()
                            .collect();
                }
            }
        """
    )
}