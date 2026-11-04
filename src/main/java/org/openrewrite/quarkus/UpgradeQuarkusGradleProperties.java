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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.*;

import static java.util.Collections.singletonList;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeQuarkusGradleProperties extends ScanningRecipe<Set<MavenRepository>> {

    private static final String PLATFORM_GROUP_ID = "quarkusPlatformGroupId";
    private static final String PLATFORM_ARTIFACT_ID = "quarkusPlatformArtifactId";
    private static final String PLATFORM_VERSION = "quarkusPlatformVersion";
    private static final String PLUGIN_VERSION = "quarkusPluginVersion";

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the Quarkus platform version.",
            example = "3.20.x")
    String newVersion;

    String displayName = "Upgrade the Quarkus versions in `gradle.properties`";

    String description = "Upgrades the `quarkusPlatformVersion` and `quarkusPluginVersion` properties in `gradle.properties`, " +
                         "which the Quarkus Gradle project template uses for the platform BOM and the Quarkus Gradle plugin, " +
                         "to the newest release of the platform BOM named by `quarkusPlatformGroupId` and " +
                         "`quarkusPlatformArtifactId` that matches the version selector. This is what `quarkus update` " +
                         "changes in such a project. A version is never downgraded.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Semver.validate(newVersion, null));
    }

    @Override
    public Set<MavenRepository> getInitialValue(ExecutionContext ctx) {
        return new LinkedHashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<MavenRepository> repositories) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree != null) {
                    // `gradle.properties` carries no build model, so look up versions where the build scripts resolve from
                    tree.getMarkers().findFirst(GradleProject.class)
                            .ifPresent(gradleProject -> repositories.addAll(gradleProject.getMavenRepositories()));
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<MavenRepository> repositories) {
        VersionComparator comparator = Semver.validate(newVersion, null).getValue();
        if (comparator == null) {
            return TreeVisitor.noop();
        }
        return Preconditions.check(new FindSourceFiles("**/gradle.properties"), new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitFile(Properties.File file, ExecutionContext ctx) {
                Map<String, String> values = new HashMap<>();
                for (Properties.Content content : file.getContent()) {
                    if (content instanceof Properties.Entry) {
                        Properties.Entry entry = (Properties.Entry) content;
                        values.put(entry.getKey(), entry.getValue().getText());
                    }
                }
                String currentVersion = values.getOrDefault(PLATFORM_VERSION, values.get(PLUGIN_VERSION));
                if (currentVersion == null) {
                    return file;
                }

                String upgradedVersion = findUpgrade(
                        values.getOrDefault(PLATFORM_GROUP_ID, "io.quarkus.platform"),
                        values.getOrDefault(PLATFORM_ARTIFACT_ID, "quarkus-bom"),
                        currentVersion,
                        ctx);
                if (upgradedVersion == null) {
                    return file;
                }

                return file.withContent(ListUtils.map(file.getContent(), content -> {
                    if (content instanceof Properties.Entry) {
                        Properties.Entry entry = (Properties.Entry) content;
                        if ((PLATFORM_VERSION.equals(entry.getKey()) || PLUGIN_VERSION.equals(entry.getKey())) &&
                            comparator.compare(null, entry.getValue().getText(), upgradedVersion) < 0) {
                            return entry.withValue(entry.getValue().withText(upgradedVersion));
                        }
                    }
                    return content;
                }));
            }

            private @Nullable String findUpgrade(String groupId, String artifactId, String currentVersion, ExecutionContext ctx) {
                List<MavenRepository> lookIn = repositories.isEmpty() ?
                        singletonList(MavenRepository.MAVEN_CENTRAL) :
                        new ArrayList<>(repositories);
                try {
                    MavenMetadata metadata = new MavenPomDownloader(ctx)
                            .downloadMetadata(new GroupArtifact(groupId, artifactId), null, lookIn);
                    return comparator.upgrade(currentVersion, metadata.getVersioning().getVersions()).orElse(null);
                } catch (MavenDownloadingException e) {
                    return null;
                }
            }
        });
    }
}
