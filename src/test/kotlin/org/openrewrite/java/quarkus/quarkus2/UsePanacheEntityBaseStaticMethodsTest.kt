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
package org.openrewrite.java.quarkus.quarkus2

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class UsePanacheEntityBaseStaticMethodsTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("quarkus-hibernate-orm-panache", "persistence-api")
        .build()

    override val recipe: Recipe
        get() = UsePanacheEntityBaseStaticMethods()

    companion object {
        private const val panacheEntityStub: String = """
            package org.openrewrite.example;
            
            import io.quarkus.hibernate.orm.panache.PanacheEntity;

            /** 
            * PanacheEntity extends PanacheEntityBase, where getEntityManager() and flush() are defined.
            * This is just a more realistic use-case.
            */
            class Person extends PanacheEntity {
            }
        """
    }

    @Test
    fun useStaticMethods() = assertChanged(
        dependsOn = arrayOf(panacheEntityStub),
        before = """
            package org.openrewrite.example;

            import org.openrewrite.example.Person;

            class Test {
                {
                    Person p = new Person();
                    p.getEntityManager();
                    p.flush();
                }
            }
        """,
        after = """
            package org.openrewrite.example;

            import org.openrewrite.example.Person;

            class Test {
                {
                    Person p = new Person();
                    Person.getEntityManager();
                    Person.flush();
                }
            }
        """
    )

}
