package org.openrewrite.java.quarkus

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class QuarkusUpgradeIntegrationTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("mutiny","mutiny-rxjava","mutiny-reactor","quarkus-mutiny", "reactive-streams")
        .build()

    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.quarkus")
        .build()
        .activateRecipes(
            "org.openrewrite.java.quarkus.Quarkus1_11to1_13Migration"
        )

    @Test
    fun changeCollection() = assertChanged(
        before = """
            import io.smallrye.mutiny.Multi;
            import io.smallrye.mutiny.Uni;
            import java.util.List;
            import java.time.Duration;
            public class A {
                public Multi<String> greetings(int count, String name) {
                    return Multi.createFrom().ticks().every(Duration.ofMillis(1))
                            .onItem().transform(n -> String.format("hello %s - %d", name, n))
                            .transform().byTakingFirstItems(count);
                }
                public Uni<List<String>> collectItems(int count, String name) {
                    Multi<String> multi = greetings(count, name);
                    Uni<List<String>> uni = multi.collectItems().asList();
                    return uni;
                }
            }
        """,
        after = """
            import io.smallrye.mutiny.Multi;
            import io.smallrye.mutiny.Uni;
            import java.util.List;
            import java.time.Duration;
            public class A {
                public Multi<String> greetings(int count, String name) {
                    return Multi.createFrom().ticks().every(Duration.ofMillis(1))
                            .onItem().transform(n -> String.format("hello %s - %d", name, n))
                            .select().first(count);
                }
                public Uni<List<String>> collectItems(int count, String name) {
                    Multi<String> multi = greetings(count, name);
                    Uni<List<String>> uni = multi.collect().asList();
                    return uni;
                }
            }
        """
    )
}