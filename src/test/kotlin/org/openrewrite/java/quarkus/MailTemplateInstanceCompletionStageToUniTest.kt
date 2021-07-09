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

class MailTemplateInstanceCompletionStageToUniTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("mutiny", "reactive-streams")
        .build()

    override val recipe: Recipe
        get() = MailTemplateInstanceCompletionStageToUni()

    @Test
    fun replaceTransform() = assertChanged(
        before = """
            package org.openrewrite.example;

            import io.quarkus.mailer.MailTemplate;

            import java.util.concurrent.CompletionStage;

            class MailTemplateTest {
                public static CompletionStage<Void> send(MailTemplate.MailTemplateInstance mailer) {
                    CompletionStage<Void> e0 = mailer.send();
                    return e0;
                }
            }
        """,
        after = """
            package org.openrewrite.example;

            import io.quarkus.mailer.MailTemplate;

            import java.util.concurrent.CompletionStage;

            class MailTemplateTest {
                public static CompletionStage<Void> send(MailTemplate.MailTemplateInstance mailer) {
                    CompletionStage<Void> e0 = mailer.send(null).subscribeAsCompletionStage();
                    return e0;
                }
            }
        """
    )

}
