/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.quarkus.quarkus2;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class JavaEEtoQuarkus2MigrationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.quarkus.quarkus2.JavaEEtoQuarkus2Migration");
    }

    @Test
    void convertJavaEEToQuarkus() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          //language=xml
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <maven.compiler.source>11</maven.compiler.source>
                  <maven.compiler.target>11</maven.compiler.target>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.quarkus.platform</groupId>
                      <artifactId>quarkus-bom</artifactId>
                      <version>2.16.12.Final</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>io.quarkus.platform</groupId>
                      <artifactId>quarkus-maven-plugin</artifactId>
                      <version>2.16.12.Final</version>
                      <executions>
                        <execution>
                          <goals>
                            <goal>build</goal>
                            <goal>generate-code</goal>
                            <goal>generate-code-tests</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.10.1</version>
                      <configuration>
                        <compilerArgs>
                          <arg>-parameters</arg>
                        </compilerArgs>
                      </configuration>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <version>3.0.0-M7</version>
                      <configuration>
                        <systemPropertyVariables>
                          <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                          <maven.home>${maven.home}</maven.home>
                        </systemPropertyVariables>
                      </configuration>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-failsafe-plugin</artifactId>
                      <version>3.0.0-M7</version>
                      <executions>
                        <execution>
                          <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                          </goals>
                        </execution>
                      </executions>
                      <configuration>
                        <systemPropertyVariables>
                          <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
                          <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                          <maven.home>${maven.home}</maven.home>
                        </systemPropertyVariables>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
                <profiles>
                  <profile>
                    <id>native</id>
                    <activation>
                      <property>
                        <name>native</name>
                      </property>
                    </activation>
                    <properties>
                      <skipITs>false</skipITs>
                      <quarkus.package.type>native</quarkus.package.type>
                    </properties>
                  </profile>
                </profiles>
              </project>
              """
          )
        );
    }
}
