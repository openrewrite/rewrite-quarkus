/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.quarkus;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

class UpgradeQuarkusVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeQuarkusVersion("3.20.x"));
    }

    @DocumentExample
    @Test
    void upgradeMavenPlatformBomAndPlugin() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0</version>
                  <properties>
                      <quarkus.platform.version>3.15.1</quarkus.platform.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>io.quarkus.platform</groupId>
                              <artifactId>quarkus-bom</artifactId>
                              <version>${quarkus.platform.version}</version>
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
                              <version>${quarkus.platform.version}</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            spec -> spec.after(pom -> assertThat(pom)
              .containsPattern("<quarkus.platform.version>3\\.20\\.\\d+(\\.\\d+)?</quarkus.platform.version>")
              .actual())
          )
        );
    }

    @Test
    void upgradeGradleProjectTemplateProperties() {
        rewriteRun(
          //language=properties
          properties(
            """
              quarkusPluginVersion=3.15.1
              quarkusPlatformVersion=3.15.1
              """,
            spec -> spec.path("gradle.properties").after(after -> assertThat(after)
              .containsPattern("quarkusPluginVersion=3\\.20\\.\\d+(\\.\\d+)?")
              .containsPattern("quarkusPlatformVersion=3\\.20\\.\\d+(\\.\\d+)?")
              .actual())
          )
        );
    }
}
