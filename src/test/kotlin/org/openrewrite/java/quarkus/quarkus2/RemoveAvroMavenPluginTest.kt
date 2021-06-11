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
import org.openrewrite.maven.MavenRecipeTest

class RemoveAvroMavenPluginTest : MavenRecipeTest {
    override val recipe: Recipe
        get() = RemoveAvroMavenPlugin()

    @Test
    fun ignoreAvroMavenPluginIfQuarkusMavenPluginNotPresent() = assertUnchanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.avro</groupId>
                    <artifactId>avro-maven-plugin</artifactId>
                    <version>1.10.0</version>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Test
    fun removeAvroMavenPluginIfQuarkusMavenPluginPresent() = assertChanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                    <version>1.13.5.Final</version>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.avro</groupId>
                    <artifactId>avro-maven-plugin</artifactId>
                    <version>1.10.0</version>
                  </plugin>
                </plugins>
              </build>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                    <version>1.13.5.Final</version>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

}