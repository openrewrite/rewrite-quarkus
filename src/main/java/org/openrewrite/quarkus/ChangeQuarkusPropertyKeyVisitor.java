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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
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
    final List<String> except;

    @Nullable
    final String profile;

    @Nullable
    final Boolean changeAllProfiles;

    @Nullable
    final List<String> pathExpressions;

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
            String keyWithoutProfile = getKeyWithoutProfile(entry.getKey());
            String transformedKey = replaceRegex(oldPropertyKey, newPropertyKey, keyWithoutProfile);
            String[] profiles = getProfiles(entry.getKey());

            if (profiles.length == 0 || Boolean.TRUE.equals(changeAllProfiles)) {
                String newKey = profiles.length == 0 ? transformedKey : "%" + String.join(",", profiles) + "." + transformedKey;
                tree = new org.openrewrite.properties.ChangePropertyKey(entry.getKey(), newKey, false, false)
                        .getVisitor()
                        .visit(tree, ctx);
            } else {
                List<String> remainingProfiles = new ArrayList<>(existingProperties.size());
                for (String profile : profiles) {
                    if (!profile.equals(this.profile)) {
                        remainingProfiles.add(profile);
                    }
                }

                // Remove the old property
                tree = new org.openrewrite.properties.DeleteProperty(entry.getKey(), false)
                        .getVisitor()
                        .visit(tree, ctx);

                // Add the new property for the named profile
                String newKey = "%" + profile + "." + transformedKey;
                tree = new org.openrewrite.properties.AddProperty(
                        newKey,
                        entry.getValue().getText(),
                        null,
                        entry.getDelimiter().getCharacter().toString()
                ).getVisitor().visit(tree, ctx);

                if (!remainingProfiles.isEmpty()) {
                    // Add the new property for the named profile
                    tree = new org.openrewrite.properties.AddProperty(
                            "%" + String.join(",", remainingProfiles) + "." + keyWithoutProfile,
                            entry.getValue().getText(),
                            null,
                            entry.getDelimiter().getCharacter().toString()
                    ).getVisitor().visit(tree, ctx);
                }
            }
        }

        return tree;
    }

    private @Nullable Tree visitYamlDocuments(Tree tree, ExecutionContext ctx) {
        Set<Yaml.Mapping.Entry> existingProperties = FindQuarkusProperties.find((Yaml.Documents) tree, oldPropertyKey, profile, changeAllProfiles);
        for (Yaml.Mapping.Entry entry : existingProperties) {
            String key = entry.getKey().getValue();
            String keyWithoutProfile = getKeyWithoutProfile(key);
            String transformedKey = replaceRegex(oldPropertyKey, newPropertyKey, keyWithoutProfile);
            List<String> remainingProfiles = new ArrayList<>();
            for (String profile : getProfiles(key)) {
                if (!profile.equals(this.profile)) {
                    remainingProfiles.add(profile);
                }
            }

            // Remove the old property
            tree = new org.openrewrite.yaml.DeleteProperty(key, false, false)
                    .getVisitor()
                    .visit(tree, ctx);

            Cursor cursor = new Cursor(getCursor(), tree);

            StringBuilder newProperties = new StringBuilder();
            if (profile != null) {
                formatKey(newProperties, transformedKey, entry.getValue().print(cursor), profile);
            } else {
                formatKey(newProperties, transformedKey, entry.getValue().print(cursor));
            }

            if (!remainingProfiles.isEmpty()) {
                formatKey(newProperties, keyWithoutProfile, entry.getValue().print(cursor), remainingProfiles.toArray(new String[0]));
            }

            // Merge the new property with the existing properties
            tree = new org.openrewrite.yaml.MergeYaml(
                    "$",
                    newProperties.toString(),
                    true,
                    null
            ).getVisitor().visit(tree, ctx);
        }

        return tree;
    }

    private static String getKeyWithoutProfile(String propertyKey) {
        if (propertyKey.isEmpty() || propertyKey.charAt(0) != '%') {
            return propertyKey;
        }
        int index = propertyKey.indexOf('.');
        if (index == -1) {
            return propertyKey;
        }
        return propertyKey.substring(index + 1);
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

    private static String[] getProfiles(String propertyKey) {
        if (propertyKey.isEmpty() || propertyKey.charAt(0) != '%') {
            return new String[0];
        }
        int index = propertyKey.indexOf('.');
        if (index == -1) {
            return new String[0];
        }
        return propertyKey.substring(1, index).split(",");
    }

    private static void formatKey(StringBuilder yaml, String property, String value, String... profiles) {
        String[] propertyParts = property.split("\\.");

        String indent = "";
        if (profiles.length > 0) {
            yaml.append("'%")
                    .append(String.join(",", profiles))
                    .append("':")
                    .append(System.lineSeparator());
            indent = indent + "  ";
        }
        for (int i = 0; i < propertyParts.length; i++) {
            String part = propertyParts[i];
            if (i > 0 && yaml.length() > 0) {
                yaml.append(System.lineSeparator());
            }
            yaml.append(indent).append(part).append(":");
            indent = indent + "  ";
        }
        yaml.append(value);
        if (yaml.length() > 0) {
            yaml.append(System.lineSeparator());
        }
    }
}
