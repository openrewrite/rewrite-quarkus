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
package org.openrewrite.quarkus;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.quarkus.search.FindQuarkusProperties;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class DeleteQuarkusPropertyVisitor extends TreeVisitor<Tree, ExecutionContext> {

    final String propertyKey;

    @Nullable
    final String oldValue;

    @Nullable
    final String profile;

    @Nullable
    final Boolean searchAllProfiles;

    @Nullable
    final List<String> pathExpressions;

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
        QuarkusExecutionContextView quarkusCtx = QuarkusExecutionContextView.view(ctx);
        return quarkusCtx.isQuarkusConfigFile(sourceFile, pathExpressions);
    }

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
        if (tree instanceof Properties.File) {
            return visitPropertiesFile(tree, ctx);
        } else if (tree instanceof Yaml.Documents) {
            return visitYamlDocuments(tree, ctx);
        }
        return tree;
    }

    private @Nullable Tree visitPropertiesFile(Tree tree, ExecutionContext ctx) {
        Set<Properties.Entry> existingProperties = FindQuarkusProperties.find((Properties) tree, propertyKey, profile, searchAllProfiles);
        for (Properties.Entry entry : existingProperties) {
            if (oldValue == null || oldValue.equals(entry.getValue().getText())) {
                String[] profiles = QuarkusProfileUtils.getProfilesFromPropertyKey(entry.getKey());

                if (profiles.length == 0 || !Boolean.FALSE.equals(searchAllProfiles)) {
                    tree = new org.openrewrite.properties.DeleteProperty(entry.getKey(), false)
                            .getVisitor()
                            .visit(tree, ctx);
                } else {
                    // Remove the old property containing the original key with multiple profiles
                    tree = new org.openrewrite.properties.DeleteProperty(entry.getKey(), false)
                            .getVisitor()
                            .visit(tree, ctx);

                    List<String> remainingProfiles = new ArrayList<>(existingProperties.size());
                    for (String profile : profiles) {
                        if (!profile.equals(this.profile)) {
                            remainingProfiles.add(profile);
                        }
                    }

                    if (!remainingProfiles.isEmpty()) {
                        String keyWithoutProfile = QuarkusProfileUtils.getKeyWithoutProfile(entry.getKey());

                        // Add a property containing the value for the remaining unmatched profiles with the original key
                        String key = "%" + String.join(",", remainingProfiles) + "." + keyWithoutProfile;
                        String value = entry.getValue().getText();
                        tree = new org.openrewrite.properties.AddProperty(
                                key,
                                value,
                                null,
                                null
                        ).getVisitor().visit(tree, ctx);
                    }
                }
            }
        }

        return tree;
    }

    private @Nullable Tree visitYamlDocuments(Tree tree, ExecutionContext ctx) {
        Set<Yaml.Mapping.Entry> existingProperties = FindQuarkusProperties.find((Yaml.Documents) tree, propertyKey, profile, searchAllProfiles);
        for (Yaml.Mapping.Entry entry : existingProperties) {
            String originalEntryValue = ((Yaml.Scalar) entry.getValue()).getValue();
            if (oldValue == null || oldValue.equals(originalEntryValue)) {
                String key = entry.getKey().getValue();

                String[] profiles = QuarkusProfileUtils.getProfilesFromPropertyKey(key);

                if (profiles.length == 0 || !Boolean.FALSE.equals(searchAllProfiles)) {
                    tree = new org.openrewrite.yaml.DeleteProperty(key, false, null)
                            .getVisitor()
                            .visit(tree, ctx);
                } else {
                    // Remove the old property containing the original key with multiple profiles
                    tree = new org.openrewrite.yaml.DeleteProperty(key, false, false)
                            .getVisitor()
                            .visit(tree, ctx);

                    List<String> remainingProfiles = new ArrayList<>();
                    for (String profile : profiles) {
                        if (!profile.equals(this.profile)) {
                            remainingProfiles.add(profile);
                        }
                    }

                    // Add a property containing the value for the unmatched profiles with the original key
                    if (!remainingProfiles.isEmpty()) {
                        StringBuilder newProperties = new StringBuilder();
                        String keyWithoutProfile = QuarkusProfileUtils.getKeyWithoutProfile(key);
                        QuarkusProfileUtils.formatKey(newProperties, keyWithoutProfile, originalEntryValue, remainingProfiles);

                        // Merge the new property with the existing properties
                        tree = new org.openrewrite.yaml.MergeYaml(
                                "$",
                                newProperties.toString(),
                                false,
                                null
                        , null).getVisitor().visit(tree, ctx);
                    }
                }
            }
        }

        return tree;
    }
}
