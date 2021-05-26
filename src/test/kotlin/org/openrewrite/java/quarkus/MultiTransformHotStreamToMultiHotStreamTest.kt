package org.openrewrite.java.quarkus

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class MultiTransformHotStreamToMultiHotStreamTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(false)
        .classpath("mutiny", "reactive-streams")
        .build()

    override val recipe: Recipe
        get() = MultiTransformHotStreamToMultiHotStream()

    @Test
    fun replaceTransform() = assertChanged(
        before = """
            import io.smallrye.mutiny.Multi;
            import java.util.List;
            import java.time.Duration;
            public class A {
                public MultiCollect<Long> hotStreamGreetings(int count, String name) {
                    return Multi.createFrom().ticks().every(Duration.ofMillis(1))
                            .transform()
                            .toHotStream()
                            .collect();
                }
            }
        """,
        after = """
            import io.smallrye.mutiny.Multi;
            import java.util.List;
            import java.time.Duration;
            public class A {
                public MultiCollect<Long> hotStreamGreetings(int count, String name) {
                    return Multi.createFrom().ticks().every(Duration.ofMillis(1))
                            .toHotStream()
                            .collect();
                }
            }
        """
    )
}