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

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.properties.PropertiesRecipeTest

class Quarkus1to113MigrationPropertiesTest : PropertiesRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.quarkus")
        .build()
        .activateRecipes("org.openrewrite.java.quarkus.Quarkus1to1_13Migration")

    @Test
    fun quarkusDevInstrumentation() = assertChanged(
        before = """
            quarkus.dev.instrumentation=true
        """,
        after = """
            quarkus.live-reload.instrumentation=true
        """
    )

}
