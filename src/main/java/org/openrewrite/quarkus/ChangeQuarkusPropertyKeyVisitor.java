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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.quarkus.search.FindQuarkusProperties;
import org.openrewrite.yaml.ShiftIndentVisitor;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptySet;

@RequiredArgsConstructor
class ChangeQuarkusPropertyKeyVisitor extends TreeVisitor<Tree, ExecutionContext> {

    private static final String VALUE_PLACEHOLDER = "__rewrite_quarkus_property_value__";

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
        }
        if (tree instanceof Yaml.Documents) {
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
                        null,
                        null,
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
                            null,
                            null,
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
        Set<String> dottedKeys = existingProperties.isEmpty() ? emptySet() : findDottedKeys((Yaml.Documents) tree);
        for (Yaml.Mapping.Entry entry : existingProperties) {
            String originalKey = entry.getKey().getValue();
            String keyWithoutProfile = QuarkusProfileUtils.getKeyWithoutProfile(originalKey);
            String transformedKey = replaceRegex(oldPropertyKey, newPropertyKey, keyWithoutProfile);
            String[] profiles = QuarkusProfileUtils.getProfilesFromPropertyKey(originalKey);

            if (profiles.length == 0 || !Boolean.FALSE.equals(changeAllProfiles)) {
                tree = replaceYamlKey(tree, ctx, originalKey, transformedKey, entry, profiles, dottedKeys);
            } else {
                List<String> remainingProfiles = new ArrayList<>(existingProperties.size());
                for (String profile : profiles) {
                    if (!profile.equals(this.profile)) {
                        remainingProfiles.add(profile);
                    }
                }

                // Remove the old property containing the original key with multiple profiles
                tree = replaceYamlKey(tree, ctx, originalKey, transformedKey, entry, profile != null ? new String[]{profile} : new String[0], dottedKeys);

                if (!remainingProfiles.isEmpty()) {
                    // Add a new property for the matched profile with the transformed key
                    tree = mergeYamlEntry(tree, ctx, keyWithoutProfile, entry, remainingProfiles.toArray(new String[0]), dottedKeys);
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

    private static Set<String> findDottedKeys(Yaml.Documents documents) {
        return new YamlIsoVisitor<Set<String>>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Set<String> dottedKeys) {
                if (entry.getKey().getValue().contains(".")) {
                    dottedKeys.add(entry.getKey().getValue());
                }
                return super.visitMappingEntry(entry, dottedKeys);
            }
        }.reduce(documents, new HashSet<>());
    }

    private static @Nullable Tree replaceYamlKey(@Nullable Tree tree, ExecutionContext ctx, String oldKey, String newKey, Yaml.Mapping.Entry entry, String[] profiles, Set<String> dottedKeys) {
        if (tree == null) {
            return null;
        }
        Tree t = new org.openrewrite.yaml.DeleteProperty(oldKey, false, null, null)
                .getVisitor()
                .visit(tree, ctx);
        return mergeYamlEntry(t, ctx, newKey, entry, profiles, dottedKeys);
    }

    /**
     * Merges the key with a placeholder value, which is then swapped for the original value, so that
     * values such as sequences, block scalars and quoted strings are carried over as-is.
     */
    private static @Nullable Tree mergeYamlEntry(@Nullable Tree tree, ExecutionContext ctx, String key, Yaml.Mapping.Entry entry, String[] profiles, Set<String> dottedKeys) {
        Tree merged = new org.openrewrite.yaml.MergeYaml(
                "$",
                QuarkusProfileUtils.formatKey(key, VALUE_PLACEHOLDER, String.join(",", profiles), dottedKeys),
                false,
                null, null, null, null, null)
                .getVisitor().visit(tree, ctx);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry e, ExecutionContext ctx) {
                if (e.getValue() instanceof Yaml.Scalar && VALUE_PLACEHOLDER.equals(((Yaml.Scalar) e.getValue()).getValue())) {
                    int shift = indent(e.getPrefix()) - indent(entry.getPrefix());
                    return e.withValue(withoutTrailingLineBreak(shiftIndent(entry.getValue(), shift)));
                }
                return super.visitMappingEntry(e, ctx);
            }
        }.visit(merged, ctx);
    }

    private static Yaml.Block shiftIndent(Yaml.Block value, int shift) {
        if (shift == 0) {
            return value;
        }
        return (Yaml.Block) new ShiftIndentVisitor<Integer>(value, shift) {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, Integer p) {
                Yaml.Scalar s = super.visitScalar(scalar, p);
                return s.withValue(shiftLines(s.getValue(), shift));
            }
        }.visitNonNull(value, 0);
    }

    /**
     * A block scalar includes the line break that ends it, which the entry following its new location already has.
     */
    private static Yaml.Block withoutTrailingLineBreak(Yaml.Block block) {
        if (block instanceof Yaml.Scalar) {
            Yaml.Scalar scalar = (Yaml.Scalar) block;
            String value = scalar.getValue();
            return value.endsWith("\n") ? scalar.withValue(value.substring(0, value.length() - (value.endsWith("\r\n") ? 2 : 1))) : scalar;
        }
        if (block instanceof Yaml.Sequence && ((Yaml.Sequence) block).getOpeningBracketPrefix() == null) {
            Yaml.Sequence sequence = (Yaml.Sequence) block;
            return sequence.withEntries(ListUtils.mapLast(sequence.getEntries(), e -> e.withBlock(withoutTrailingLineBreak(e.getBlock()))));
        }
        if (block instanceof Yaml.Mapping && ((Yaml.Mapping) block).getOpeningBracePrefix() == null) {
            Yaml.Mapping mapping = (Yaml.Mapping) block;
            return mapping.withEntries(ListUtils.mapLast(mapping.getEntries(), e -> e.withValue(withoutTrailingLineBreak(e.getValue()))));
        }
        return block;
    }

    private static String shiftLines(String text, int shift) {
        String[] lines = text.split("\\n", -1);
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (StringUtils.isBlank(line)) {
                continue;
            }
            if (shift > 0) {
                lines[i] = StringUtils.repeat(" ", shift) + line;
            } else if (StringUtils.indexOfNonWhitespace(line) >= -shift) {
                lines[i] = line.substring(-shift);
            }
        }
        return String.join("\n", lines);
    }

    private static int indent(String prefix) {
        int lineBreak = prefix.lastIndexOf('\n');
        return lineBreak < 0 ? 0 : prefix.length() - lineBreak - 1;
    }
}
