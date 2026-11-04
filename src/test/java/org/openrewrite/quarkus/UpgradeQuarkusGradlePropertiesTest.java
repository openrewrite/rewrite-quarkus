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
import static org.openrewrite.properties.Assertions.properties;

class UpgradeQuarkusGradlePropertiesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeQuarkusGradleProperties("3.20.x"));
    }

    @DocumentExample
    @Test
    void upgradeQuarkusProjectTemplateProperties() {
        rewriteRun(
          //language=properties
          properties(
            """
              quarkusPluginId=io.quarkus
              quarkusPluginVersion=3.15.1
              quarkusPlatformGroupId=io.quarkus.platform
              quarkusPlatformArtifactId=quarkus-bom
              quarkusPlatformVersion=3.15.1
              """,
            spec -> spec.path("gradle.properties").after(after -> assertThat(after)
              .containsPattern("(?ms)quarkusPluginVersion=(3\\.20\\.\\d+(\\.\\d+)?)$.*quarkusPlatformVersion=\\1$")
              .contains("quarkusPlatformGroupId=io.quarkus.platform")
              .actual())
          )
        );
    }

    @Test
    void lookUpTheNamedPlatformBom() {
        rewriteRun(
          //language=properties
          properties(
            """
              quarkusPlatformGroupId=io.quarkus
              quarkusPlatformArtifactId=quarkus-bom
              quarkusPlatformVersion=3.15.1
              """,
            spec -> spec.path("gradle.properties").after(after -> assertThat(after)
              .containsPattern("quarkusPlatformVersion=3\\.20\\.\\d+(\\.\\d+)?$")
              .actual())
          )
        );
    }

    @Test
    void upgradePluginVersionBehindPlatformVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeQuarkusGradleProperties("3.20.0")),
          //language=properties
          properties(
            """
              quarkusPluginVersion=3.15.1
              quarkusPlatformVersion=3.20.0
              """,
            """
              quarkusPluginVersion=3.20.0
              quarkusPlatformVersion=3.20.0
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void doNotDowngrade() {
        rewriteRun(
          //language=properties
          properties(
            """
              quarkusPluginVersion=3.21.0
              quarkusPlatformVersion=3.21.0
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void onlyGradleProperties() {
        rewriteRun(
          //language=properties
          properties(
            """
              quarkusPlatformVersion=3.15.1
              """,
            spec -> spec.path("src/main/resources/application.properties")
          )
        );
    }

    @Test
    void noQuarkusVersions() {
        rewriteRun(
          //language=properties
          properties(
            """
              org.gradle.caching=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }
}
