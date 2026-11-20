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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.java.dependencies.UpgradeDependencyVersion;
import org.openrewrite.maven.UpgradePluginVersion;
import org.openrewrite.semver.Semver;

import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeQuarkusVersion extends Recipe {

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the Quarkus platform version.",
            example = "3.20.x")
    String newVersion;

    String displayName = "Upgrade Quarkus version";

    String description = "Upgrades the Quarkus platform BOM, the `quarkus-maven-plugin`, the `io.quarkus` Gradle plugin " +
                         "and the Quarkus versions in `gradle.properties`, as `quarkus update` does before it runs any " +
                         "update recipes. A version is never downgraded.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Semver.validate(newVersion, null));
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new UpgradeDependencyVersion("io.quarkus.platform", "quarkus-bom", newVersion, null, null, null),
                new UpgradeDependencyVersion("io.quarkus", "quarkus-bom", newVersion, null, null, null),
                new UpgradeDependencyVersion("io.quarkus", "quarkus-universe-bom", newVersion, null, null, null),
                new UpgradePluginVersion("io.quarkus*", "quarkus-maven-plugin", newVersion, null, null, null),
                new org.openrewrite.gradle.plugins.UpgradePluginVersion("io.quarkus", newVersion, null),
                new UpgradeQuarkusGradleProperties(newVersion)
        );
    }
}
