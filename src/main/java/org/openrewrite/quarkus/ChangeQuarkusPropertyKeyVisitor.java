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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.quarkus.search.FindQuarkusProperties;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
class ChangeQuarkusPropertyKeyVisitor extends TreeVisitor<Tree, ExecutionContext> {

    final String oldPropertyKey;

    final String newPropertyKey;

    @Nullable
    final String profile;

    @Nullable
    final Boolean changeAllProfiles;

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
        Set<Properties.Entry> existingProperties = FindQuarkusProperties.find((Properties) tree, oldPropertyKey, profile, changeAllProfiles);
        for (Properties.Entry entry : existingProperties) {
            String keyWithoutProfile = QuarkusProfileUtils.getKeyWithoutProfile(entry.getKey());
            String transformedKey = replaceRegex(oldPropertyKey, newPropertyKey, keyWithoutProfile);
            String[] profiles = QuarkusProfileUtils.getProfilesFromPropertyKey(entry.getKey());

            if (profiles.length == 0 || !Boolean.FALSE.equals(changeAllProfiles)) {
                String newKey = profiles.length == 0 ? transformedKey : "%" + String.join(",", profiles) + "." + transformedKey;
                tree = new org.openrewrite.properties.ChangePropertyKey(entry.getKey(), newKey, false, false)
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


                // Add a new property for the matched profile with the transformed key
                String key = "%" + profile + "." + transformedKey;
                tree = new org.openrewrite.properties.AddProperty(
                        key,
                        entry.getValue().getText(),
                        null,
                        null
                ).getVisitor().visit(tree, ctx);

                if (!remainingProfiles.isEmpty()) {
                    // Add a property containing the value for the remaining unmatched profiles with the original key
                    key = "%" + String.join(",", remainingProfiles) + "." + keyWithoutProfile;
                    tree = new org.openrewrite.properties.AddProperty(
                            key,
                            entry.getValue().getText(),
                            null,
                            null
                    ).getVisitor().visit(tree, ctx);
                }
            }
        }

        return tree;
    }

    private @Nullable Tree visitYamlDocuments(Tree tree, ExecutionContext ctx) {
        Set<Yaml.Mapping.Entry> existingProperties = FindQuarkusProperties.find((Yaml.Documents) tree, oldPropertyKey, profile, changeAllProfiles);
        for (Yaml.Mapping.Entry entry : existingProperties) {
            String originalEntryValue = ((Yaml.Scalar) entry.getValue()).getValue();
            String originalKey = entry.getKey().getValue();
            String keyWithoutProfile = QuarkusProfileUtils.getKeyWithoutProfile(originalKey);
            String transformedKey = replaceRegex(oldPropertyKey, newPropertyKey, keyWithoutProfile);
            String[] profiles = QuarkusProfileUtils.getProfilesFromPropertyKey(originalKey);

            if (profiles.length == 0 || !Boolean.FALSE.equals(changeAllProfiles)) {
                tree = replaceYamlKey(tree, ctx, originalKey, transformedKey, originalEntryValue, profiles);
            } else {
                List<String> remainingProfiles = new ArrayList<>(existingProperties.size());
                for (String profile : profiles) {
                    if (!profile.equals(this.profile)) {
                        remainingProfiles.add(profile);
                    }
                }

                // Remove the old property containing the original key with multiple profiles
                tree = replaceYamlKey(tree, ctx, originalKey, transformedKey, originalEntryValue, profile != null ? new String[]{profile} : new String[0]);

                if (!remainingProfiles.isEmpty()) {
                    // Add a new property for the matched profile with the transformed key
                    tree = new org.openrewrite.yaml.MergeYaml(
                            "$",
                            QuarkusProfileUtils.formatKey(keyWithoutProfile, originalEntryValue, String.join(",", remainingProfiles)),
                            false,
                            null
                    , null).getVisitor().visit(tree, ctx);
                }
            }
        }

        return tree;
    }

    private static String replaceRegex(String oldRegex, String newRegex, String input) {
        Matcher matcher = Pattern.compile(oldRegex).matcher(input);
        if (matcher.find()) {
            StringBuffer result = new StringBuffer();
            do {
                matcher.appendReplacement(result, newRegex);
            } while (matcher.find());
            matcher.appendTail(result);
            return result.toString();
        }
        return input;
    }

    private static @Nullable Tree replaceYamlKey(@Nullable Tree tree, ExecutionContext ctx, String oldKey, String newKey, String value, String[] profiles) {
        if (tree == null) {
            return null;
        }
        Tree t = new org.openrewrite.yaml.DeleteProperty(oldKey, false, null, null)
                .getVisitor()
                .visit(tree, ctx);
        return new org.openrewrite.yaml.MergeYaml(
                "$",
                QuarkusProfileUtils.formatKey(newKey, value, String.join(",", profiles)),
                false,
                null
        , null).getVisitor().visit(t, ctx);
    }
}
