/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.quarkus.search;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

class FindQuarkusProfilesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindQuarkusProfiles());
    }

    @Test
    void propertiesFile() {
        @Language("properties")
        String source = """
          quarkus.http.port=8080
          %dev.quarkus.http.port=9090
          %staging,prod.quarkus.http.root-path=/quarkus
          """;

        Properties.File sourceFile = PropertiesParser.builder().build().parse(source)
          .filter(Properties.File.class::isInstance)
          .map(Properties.File.class::cast)
          .findFirst()
          .orElseThrow();

        assertThat(FindQuarkusProfiles.find(sourceFile))
          .containsExactlyInAnyOrder("dev", "staging", "prod");
    }

    @Test
    void yamlFile() {
        @Language("yml")
        String source = """
          quarkus:
            http:
              port: 8080
          "%dev":
            quarkus:
              http:
                port: 9090
          "%staging,prod":
            quarkus:
              http:
                root-path: /quarkus
          """;

        Yaml.Documents sourceFile = YamlParser.builder().build().parse(source)
          .filter(Yaml.Documents.class::isInstance)
          .map(Yaml.Documents.class::cast)
          .findFirst()
          .orElseThrow();

        assertThat(FindQuarkusProfiles.find(sourceFile))
          .containsExactlyInAnyOrder("dev", "staging", "prod");
    }
}
